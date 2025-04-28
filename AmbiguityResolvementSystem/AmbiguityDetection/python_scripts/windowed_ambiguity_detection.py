# Resource: https://github.com/faust-streaming/faust/blob/master/examples/windowing/hopping.py
# Used the example partially -> only for the windowing part
from datetime import datetime
import faust
import xml.etree.ElementTree as ET
import yaml

def load_config(path):
    with open(path, "r") as f:
        return yaml.safe_load(f)

# load config file
config = load_config("../adm-config.yaml")
app = faust.App('xes-windowing', 
    broker=config['ambiguity_detection']['kafka_broker']
)

class RawXesEvent(faust.Record, serializer='raw'):
    data: bytes

TOPIC = config['ambiguity_detection']['topic']

hopping_topic = app.topic(TOPIC, value_type=RawXesEvent)
# defining table with hopping window
hopping_table = app.Table(
    'hopping_table',
    default=int,
    partitions=1
).hopping(
    size=config['ambiguity_detection']['window_size'], # 60 seconds windows
    step=config['ambiguity_detection']['window_slide'],  # new window every 10 seconds
    expires=config['ambiguity_detection']['window_expiration'] # retain old windows for 150 seconds
)

@app.agent(hopping_topic)
async def process_xes(events):
    print("Started processing event")
    async for raw_event in events:
        #resource: https://docs.python.org/3/library/xml.etree.elementtree.html
        xml_str = raw_event.data.decode('utf-8')
        try:
            root = ET.fromstring(xml_str)

            parsed = {}
            for child in root:
                key=child.attrib.get('key')
                value=child.attrib.get('value')
                parsed[key] = value

            activity = parsed.get("concept:name")
            str_timestamp = parsed.get("time:timestamp")

            # check if activity and str_timestamp have a value
            if not activity or not str_timestamp:
                continue

            event_time = datetime.fromisoformat(str_timestamp.replace("Z", "+00:00"))
            # truncate timestamp to compare timestamps for ambiuity --> in case latency occurs 
            key = event_time.strftime('%Y-%m-%dT%H:%M:%S')

            # increment count for this timestamp
            hopping_table[key] += 1
            print(f"[{event_time}] Activity: {activity} -> Count in window: {hopping_table[key]}")

            if hopping_table[key]>1:
                print(f"Ambiguity detected for {hopping_table[key]} events: {parsed}")
                print("Sending events to Orchestrator to resolve ambiguity...")
        except ET.ParseError as e:
            print(f"Failed to parse XES: {e}")

if __name__ == '__main__':
    app.main()