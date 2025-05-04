import numpy as np
import cv2
from ml_model import model # import fine-tuned model
import os
import glob
import pathlib
import random
import logging

def model_inference (frame_paths):
    # frame paths is a list so we check if it's empty
    if not frame_paths:
        return {"error": "No image found!"}
    logging.info("Infering with model...")
    result = model(frame_paths)
    formatted_result = format_model_output(result, frame_paths)
    return formatted_result

# I used ChatGPT to help me format the model's output
def format_model_output(results, frame_paths):
    if not results:
        logging.error("Model returned no results!")
        return {"error": "Empty result"}

    all_probabilities = []

    for result in results: 
        if not hasattr(result, "probs") or result.probs is None:
            logging.error("No probability returned by the model.")
            continue
        all_probabilities.append(result.probs.data.tolist())

    if not all_probabilities:
        logging.error("No valid probabilities returned by the model.")
        return {"error": "No valid probabilities returned by model!"}

    avg_prob = np.mean(all_probabilities, axis=0)
    class_names = results[0].names
    class_probs = {
        class_names[i]: float(prob) for i, prob in enumerate(avg_prob)
    }

    top_class = max(class_probs, key=class_probs.get)
    top_confidence = class_probs[top_class]
    logging.info("Probabilities returned by the model: %s", class_probs)
    return {
        "top_class": top_class,
        "confidence": top_confidence,
        "all_class_probabilities": class_probs,
        "frame_paths": frame_paths
    }