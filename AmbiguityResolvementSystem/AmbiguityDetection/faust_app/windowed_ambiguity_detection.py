# command: faust -A windowed_ambiguity_detection worker -l info
from datetime import datetime
import time
import faust
import xml.etree.ElementTree as ET
import yaml
from typing import Optional
from xes_to_json_mapper import parse_xml
import asyncio
import requests
import logging

# load configurations
def load_config(path):
    with open(path, "r") as f:
        return yaml.safe_load(f)

# Reference: https://medium.com/@goldengrisha/mastering-windowing-in-faust-tumbling-hopping-and-sliding-explained-with-examples-31bf5d5a1bd4
# adjusted code to my need
# load config file
config = load_config("../adm-config.yaml")
app = faust.App('ambiguity_detection', broker=config ['ambiguity_detection']['kafka_broker'])

TOPIC = config['ambiguity_detection']['topic']

buffer = []
last_seen = 0
timeout =3 # 1 second ambiguity window

@app.agent(TOPIC)
async def process(event_stream):
    async for event in event_stream.events():
        # event assumes a key-value pair, so we just take the value
        print(f"Received: {event.value}")
        global last_seen, buffer, trigger_task
        buffer.append(event.value)
        last_seen = time.time()

        # if there is a task running when a new event occured cancel it --> ambiguity
        if trigger_task is not None and not trigger_task.done():
            trigger_task.cancel()

        # inactivity indicated that 
        trigger_task = asyncio.create_task(trigger_on_inactivity())

async def trigger_on_inactivity():
    await asyncio.sleep(timeout)
    if time.time() - last_seen >= timeout:
        if len(buffer)>1:
            logging.info(f"Ambiguity detected. Sending {len(buffer)} events to resolve the ambiguity...")
            send_to_orchestrator("ambiguous-event", {"events", buffer})
        elif len(buffer)==1:
            logging.info("Single event detected. Sending to publisher.")  
            send_to_orchestrator("unambiguous-event", {"events", buffer[0]})
        buffer.clear()

def send_to_orchestrator(endpoint, data):
    url = f"http://localhost/orchestrate/{endpoint}"
    try:
        response = requests.post(url, json=data)
        if response.status_code == 200:
            logging.info(f"Successfully sent to orchestrator with endpoint {endpoint}.")
        else: 
            logging.error(f"Failed to send to orchestrator with endpoint {endpoint}, Status {response.status_code}")
    except requests.exceptions.RequestException as e:
        logging.error(f"Error occurred when sending to orchestrator with {endpoint}: {str(e)}")


if __name__=="__main__":
    app.main()