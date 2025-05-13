# command: faust -A windowed_ambiguity_detection worker -l info
from datetime import datetime
import time
import faust
import xml.etree.ElementTree as ET
import yaml
from typing import Optional
from dateutil import parser
import sys
import os
# tells python to look one level up from the current file's directory
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from xes_to_json_mapper import parse_xml
import asyncio
import requests
import logging


logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")

# load configurations
config_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "adm-config.yaml")
with open(config_path, "r") as file:
    config = yaml.safe_load(file)

# Reference: https://medium.com/@goldengrisha/mastering-windowing-in-faust-tumbling-hopping-and-sliding-explained-with-examples-31bf5d5a1bd4
# adjusted code to my need
app = faust.App('ambiguity_detection', broker=config['kafka_conf']['kafka_broker_faust'])
TOPIC = config['kafka_conf']['topic']

buffer = []
last_seen = 0
timeout =1 # 1 second ambiguity window
trigger_task = None

@app.agent(TOPIC)
async def process(event_stream):
    global last_seen, buffer, trigger_task
    async for event in event_stream.events():
        # event assumes a key-value pair, so we just take the value
        logging.info(f"Received: {event.value}")

        try: 
            # get event timestamp from the JSON
            event_data = event.value
            event_timestamp_str = event_data.get("time:timestamp")
            if event_timestamp_str is None:
                logging.warning("No timestamp found in event, skipping")
                continue

            # parse timestamp to timestamp
            last_seen = time.time()
            buffer.append(event_data)

            # cancel previous inactivity task
            if trigger_task is not None and not trigger_task.done():
                trigger_task.cancel()

            # inactivity indicates no event stream --> process buffer ((buffer.size>1) == ambiguity) 
            trigger_task = asyncio.create_task(trigger_on_inactivity())
        except Exception as e:
            logging.error(f"Error processing event: {e}")

async def trigger_on_inactivity():
    await asyncio.sleep(timeout)
    # process buffer if new events came before timeout
    if len(buffer)>0:
        process_buffer()

def process_buffer():
    if len(buffer)>1:
        logging.info(f"Ambiguity detected. Sending {len(buffer)} events to orchestrator to resolve the ambiguity...")
        send_to_orchestrator("ambiguous-event", {"events": buffer})
    elif len(buffer)==1:
        logging.info("No ambiguity detected. Sending to publisher.")  
        send_to_orchestrator("unambiguous-event", {"events": buffer[0]})
    buffer.clear()

def send_to_orchestrator(endpoint, data):
    url = f"http://localhost:8080/orchestrate/{endpoint}"
    try:
        response = requests.post(url, json=data)
        if response.status_code == 200:
            logging.info(f"Successfully sent to orchestrator at {endpoint}.")
        else: 
            logging.error(f"Failed to send to orchestrator at {endpoint}, Status {response.status_code}")
    except requests.exceptions.RequestException as e:
        logging.error(f"Error occurred when sending to orchestrator at {endpoint}: {str(e)}")

if __name__=="__main__":
    app.main()