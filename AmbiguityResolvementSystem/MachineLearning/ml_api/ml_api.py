from fastapi import FastAPI
from fastapi.responses import JSONResponse
from ml_inference import model_inference, format_model_output
from pydantic import BaseModel
from typing import List
# from ml_model import model 

# command line to run the system: uvicorn ml_api:service --host 0.0.0.0. --port 8001

service = FastAPI()

class FramePaths(BaseModel):
    frame_paths: List[str]

@service.post("/analyze-frames")
def analyze_frames(data: FramePaths):
    try: 
        result = model_inference(data.frame_paths)
        return {"status": "success", "result": result}
    except Exception as e:
        return JSONResponse(status_code=500, content={"error": str(e)})