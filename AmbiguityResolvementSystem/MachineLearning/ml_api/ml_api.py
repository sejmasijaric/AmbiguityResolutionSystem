from fastapi import FastAPI
from fastapi.responses import JSONResponse
from ml_inference import model_inference, format_model_output
# from ml_model import model 

# command line to run the system:

service = FastAPI()

@service.post("/analyze-frames")
def analyze_frames():
    try: 
        result = model_inference()
        return {"status": "success", "result": result}
    except Exception as e:
        return JSONResponse(status_code=500, content={"error": str(e)})