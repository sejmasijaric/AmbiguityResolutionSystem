import pytest
from unittest.mock import patch, MagicMock
import requests
import asyncio
import sys
import os
# for some reason pytest won't find the module without this
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from faust_app.windowed_ambiguity_detection import process, send_to_orchestrator

@patch("faust_app.windowed_ambiguity_detection.send_to_orchestrator")
@patch("faust_app.windowed_ambiguity_detection.asyncio.create_task")
def test_detect_ambiguity_when_multiple_events_in_buffer(mock_create_task, mock_send_to_orchestrator):
    mock_event_stream = MagicMock()
    mock_event_stream.events.return_value = [
        MagicMock(value={'concept:name': 'Take out samples', 'time:timestamp': '2024-09-11T16:00:52', 'perform:hcw': 'HCW0003', 'location:station': 'Left station'}),
        MagicMock(value={'concept:name': 'HCW check-out', 'time:timestamp': '2024-09-11T16:01:24', 'perform:hcw': 'HCW0003', 'location:station': 'Left station'})
    ]

    mock_create_task.return_value = MagicMock(done=lambda: False)

    asyncio.run(process(mock_event_stream))

    mock_send_to_orchestrator.assert_called_once_with(
        "ambiguous-event", {"events": [
            {'concept:name': 'Take out samples', 'time:timestamp': '2024-09-11T16:00:52', 'perform:hcw': 'HCW0003', 'location:station': 'Left station'},
            {'concept:name': 'HCW check-out', 'time:timestamp': '2024-09-11T16:01:24', 'perform:hcw': 'HCW0003', 'location:station': 'Left station'}
        ]}
    )

@patch("faust_app.windowed_ambiguity_detection.send_to_orchestrator")
@patch("faust_app.windowed_ambiguity_detection.asyncio.create_task")
def test_send_single_event_when_no_ambiguity(mock_create_task, mock_send_to_orchestrator):
    mock_event_stream = MagicMock()
    mock_event_stream.events.return_value = [
        MagicMock(value={'concept:name': 'HCW check-out', 'time:timestamp': '2024-09-11T16:01:24', 'perform:hcw': 'HCW0003', 'location:station': 'Left station'})
    ]

    mock_create_task.return_value = MagicMock(done=lambda: False)

    asyncio.run(process(mock_event_stream))

    mock_send_to_orchestrator.assert_called_once_with(
        "unambiguous-event", {"events": {'concept:name': 'HCW check-out', 'time:timestamp': '2024-09-11T16:01:24', 'perform:hcw': 'HCW0003', 'location:station': 'Left station'}}
    )

@patch("faust_app.windowed_ambiguity_detection.logging.warning")
def test_skip_event_without_timestamp(mock_logging_warning):
    mock_event_stream = MagicMock()
    mock_event_stream.events.return_value = [
        MagicMock(value={"key": "value"})
    ]

    asyncio.run(process(mock_event_stream))

    mock_logging_warning.assert_called_once_with("No timestamp found in event, skipping")

@patch("faust_app.windowed_ambiguity_detection.logging.error")
def test_logs_error_on_invalid_timestamp(mock_logging_error):
    mock_event_stream = MagicMock()
    mock_event_stream.events.return_value = [
        MagicMock(value={"time:timestamp": "invalid-timestamp"})
    ]

    asyncio.run(process(mock_event_stream))

    mock_logging_error.assert_called_once_with(
        "Failed to parse timestamp from event: Invalid isoformat string: 'invalid-timestamp'"
    )

@patch("faust_app.windowed_ambiguity_detection.requests.post")
@patch("faust_app.windowed_ambiguity_detection.logging.error")
def test_logs_error_when_orchestrator_request_fails(mock_logging_error, mock_requests_post):
    mock_requests_post.side_effect = requests.exceptions.RequestException("Connection error")

    send_to_orchestrator("unambiguous-event", {"events": {'concept:name': 'HCW check-out', 'time:timestamp': '2024-09-11T16:01:24', 'perform:hcw': 'HCW0003', 'location:station': 'Left station'}})

    mock_logging_error.assert_called_once_with(
        "Error occurred when sending to orchestrator at unambiguous-event: Connection error"
    )

@patch("faust_app.windowed_ambiguity_detection.requests.post")
@patch("faust_app.windowed_ambiguity_detection.logging.info")
def test_logs_info_when_orchestrator_request_succeeds(mock_logging_info, mock_requests_post):
    mock_requests_post.return_value.status_code = 200

    send_to_orchestrator("unambiguous-event", {
        "events": {'concept:name': 'HCW check-out', 'time:timestamp': '2024-09-11T16:01:24',
                   'perform:hcw': 'HCW0003', 'location:station': 'Left station'}})

    mock_logging_info.assert_called_once_with(
        "Successfully sent to orchestrator at unambiguous-event."
    )