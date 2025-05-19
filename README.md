# Ambiguity Resolution System
This project is a modular, event-driven system designed to resolve ambiguity in IoT-based activity detection using machine learning. It listens to real-time IoT event streams, detects ambiguous events, and resolves them using image classification from on-demand camera input.

## System Overview

The system performs the following steps:
- Listens to IoT events via MQTT.
- Detects ambiguity using a Faust-based session window.
- Captures image frames from the environment when needed.
- Classifies activity with a fine-tuned YOLOv11 image classifier.
- Republishes events to:
  - clean-events (disambiguated or unambiguous)
  - ambiguous-events (awaiting manual intervention)

## Technologies Used

**Java 21** + Spring Boot (backend modules)

**Python 3.9 / 3.13** (streaming, ML, camera control)

**MQTT & Kafka** (messaging protocols)

**FastAPI + Uvicorn** (REST APIs for ML & camera)

**YOLOv11n-cls** via Ultralytics

**Faust** (Python stream processor)

**Docker** (Kafka broker)

**Maven** (Java build system)

## Getting Started

#### 1. Clone the Repository

```bash 
git clone https://github.com/sejmasijaric/AmbiguityResolutionSystem.git 
cd AmbiguityResolvementSystem/AmbiguityResolutionSystem
```

#### 2. Requirements
- Java 21
- Python 3.9 (for Faust stream processing) and 3.13 (ML, camera control)
- Maven
- Docker (for Kafka)
- A camera above the process environment (for capturing frames)

#### 3. Start your Kafka broker in a Docker container

```bash
# Docker must be running
cd AmbiguityDetection
docker compose up
```

#### 4. Set up Python environments
Create 2 virtual environments for the two python versions at project root
```bash
python3 -m venv venv313
source venv313/bin/activate
pip install -r requirements.txt
```
Open a new terminal and run:
``` bash
python3.9 -m venv venv39
source venv39/bin/activate
cd AmbiguityResolvementSystem/AmbiguityDetection/faust_app
pip install -r requirements_faust.txt
```
#### 2. Run the Services and Bridge
 Open new terminals for the services and run from the root directory where the virtual environments are located. 

For the Camera Control server:
```bash
source venv313/bin/activate
cd AmbiguityResolvementSystem/CameraControl/camera_api
uvicorn camera_control:app --host 0.0.0.0 --port 8000
```
For the Machine Learning server:
```bash
source venv313/bin/activate
cd AmbiguityResolvementSystem/MachineLearning/ml_api
uvicorn ml_api:service --host 0.0.0.0 --port 8001
```
For the MQTT-Kafka bridge:
```
source venv313/bin/activate
cd AmbiguityResolvementSystem/AmbiguityDetection
python3 mqtt_kafka_bridge.py
```
For the Faust application:
```
source venv39/bin/activate
cd AmbiguityResolvementSystem/AmbiguityDetection/faust_app
faust -A windowed_ambiguity_detection worker -l info
```

Finally, start the Spring Boot application through the Java Orchestrator.

## Testing
Unit and integration tests are implemented using:

- Pytest (Python)
- JUnit 5 and Mockito (Java)

## Modules
Each module has a single responsibility, communicating via REST APIs or interfaces. This ensures modularity and minimizes coupling.

## Future work
- Integrate a UI for manual ambiguity resolution
- Support concurrent activity detection
- Optimize image capturing and processing latency
- Expand ML model to more activity class labels
