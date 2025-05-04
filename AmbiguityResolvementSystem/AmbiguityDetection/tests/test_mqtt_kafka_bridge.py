import os
import sys
import pytest
from unittest.mock import MagicMock, patch

# Step 1: Create dummy config file before importing the module
CONFIG_PATH = os.path.join(os.path.dirname(__file__), '..', 'adm-config.yaml')
CONFIG_CONTENT = """
mqtt_conf:
  server: "test.mosquitto.org"
  tcp_port: 1883
  topic: "iot/test"
  FIRST_RECONNECT_DELAY: 1
  RECONNECT_RATE: 2
  MAX_RECONNECT_COUNT: 3
  MAX_RECONNECT_DELAY: 4

kafka_conf:
  kafka_broker: "localhost:9092"
  topic: "iot-events"
"""

@pytest.fixture(scope="module", autouse=True)
def setup_config():
    with open(CONFIG_PATH, "w") as f:
        f.write(CONFIG_CONTENT)
    yield
    os.remove(CONFIG_PATH)

# Step 2: Add parent directory to path so we can import mqtt_kafka_bridge
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
import mqtt_kafka_bridge  # only after the config exists!

@patch("mqtt_kafka_bridge.parse_xml")
@patch("mqtt_kafka_bridge.producer")
def test_mqtt_message_sent_to_kafka(mock_producer, mock_parse_xml):
    mock_client = MagicMock()
    mock_msg = MagicMock()
    mock_msg.payload = b'<event><string key="concept:name" value="TestActivity"/></event>'
    mock_msg.topic = "iot/test"

    mock_parse_xml.return_value = {"concept:name": "TestActivity"}

    mqtt_kafka_bridge.subscribe(mock_client)
    mock_client.on_message(mock_client, None, mock_msg)

    mock_parse_xml.assert_called_once()
    mock_producer.produce.assert_called_once()
    mock_producer.flush.assert_called_once()
