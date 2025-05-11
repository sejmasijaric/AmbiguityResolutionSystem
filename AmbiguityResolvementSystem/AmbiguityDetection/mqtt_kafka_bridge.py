import logging
import time
from paho.mqtt import client as mqtt_client
import yaml
from xes_to_json_mapper import parse_xml
import random
from confluent_kafka import Producer
import json


# resource: https://www.emqx.com/en/blog/how-to-use-mqtt-in-python
# for connection to broker and subscription to topic
def load_config(path):
    with open(path, "r") as f:
        return yaml.safe_load(f)

config = load_config("adm-config.yaml")

# mqtt configurations
BROKER = config['mqtt_conf']['server']
PORT = config['mqtt_conf']['tcp_port']
BROKER_TOPIC = config['mqtt_conf']['topic']
client_id = f'python-mqtt-{random.randint(0, 1000)}'
FIRST_RECONNECT_DELAY = config['mqtt_conf']['FIRST_RECONNECT_DELAY']
RECONNECT_RATE = config['mqtt_conf']['RECONNECT_RATE']
MAX_RECONNECT_COUNT = config['mqtt_conf']['MAX_RECONNECT_COUNT']
MAX_RECONNECT_DELAY = config['mqtt_conf']['MAX_RECONNECT_DELAY']

# kafka configurations
KAFKA_BOOTSTRAP_SERVER = config['kafka_conf']['kafka_broker']
KAFKA_TOPIC = config['kafka_conf']['topic']
producer_conf = {
    'bootstrap.servers': KAFKA_BOOTSTRAP_SERVER
}
producer = Producer(producer_conf)


def connect_mqtt():
    def on_connect(client, userdata, flags, rc, properties):
        if rc == 0:
            logging.info("Connected to MQTT Broker!")
        else:
            logging.info("Failed to connect to Broker, return code: %d", rc)

    client = mqtt_client.Client(client_id=client_id, callback_api_version=mqtt_client.CallbackAPIVersion.VERSION2)
    client.on_connect = on_connect
    client.on_disconnect = on_disconnect
    client.connect(BROKER, PORT)
    client.reconnect_delay_set(min_delay=FIRST_RECONNECT_DELAY, max_delay=MAX_RECONNECT_DELAY)
    
    return client

def on_disconnect(client, userdata, rc, properties=None):
    logging.info("Disconnected with result code: %s", rc)
    reconnect_count, reconnect_delay = 0, FIRST_RECONNECT_DELAY
    while reconnect_count < MAX_RECONNECT_COUNT:
        logging.info("Reconnecting in %d seconds...", reconnect_delay)
        time.sleep(reconnect_delay)

        try:
            client.reconnect()
            logging.info("Reconnected successfully!")
            return
        except Exception as err:
            logging.error("%s. Reconnect failed. Retrying...", err)

        reconnect_delay *= RECONNECT_RATE
        reconnect_delay = min(reconnect_delay, MAX_RECONNECT_DELAY)
        reconnect_count += 1
    logging.info("Reconnect failed after %s attempts. Exiting...", reconnect_count)

def subscribe(client: mqtt_client):
    def on_message(client, userdata, msg):
        logging.info(f"Received `{msg.payload.decode()}` from `{msg.topic}` topic")
        try:
            message = msg.payload
            json_event = parse_xml(message) # this is a dictionary of the event (JSON format)
            if json_event is not None:
                json_event_raw = json.dumps(json_event).encode('utf-8') # necessary because kafka expects a bytes-like object
                producer.produce(KAFKA_TOPIC, value=json_event_raw, callback=delivery_report)
                producer.flush()
        except Exception as err:
            logging.error("Failed to send to Kafka", err)

    client.subscribe(BROKER_TOPIC)
    client.on_message = on_message

def delivery_report(err, msg):
    if err is not None:
        logging.error("Event message delivery failed: ", err)
    else: 
        logging.info("Event message delivered to topic: %s, partition: %s", msg.topic(), msg.partition())

def run():
    client = connect_mqtt()
    subscribe(client)
    client.loop_start()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        logging.info("Disconnecting...")
        client.loop_stop()
        client.disconnect()
        producer.flush()


if __name__ == '__main__':
    run()