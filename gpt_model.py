import cv2
import os
import random
import shutil

# Define paths
init_data_path = "/Users/sejmasijaric/Documents/Bachelor Thesis/HandGestureTrainingData"
classified_data_path = "/Users/sejmasijaric/Documents/Bachelor Thesis/HandGestureTrainingDataClassified"

# Define labels
labels = ["desinfection", "injection", "wrapping"]

# Define train/val split ratio
train_ratio = 0.8  # 80% training, 20% validation
frame_skip = 5  # Save every 5th frame

# Create main classified folder
train_path = os.path.join(classified_data_path, "train")
val_path = os.path.join(classified_data_path, "val")

os.makedirs(train_path, exist_ok=True)
os.makedirs(val_path, exist_ok=True)

# Create subfolders for labels inside 'train' and 'val'
for label in labels:
    os.makedirs(os.path.join(train_path, label), exist_ok=True)
    os.makedirs(os.path.join(val_path, label), exist_ok=True)

# Process each label folder
for label in labels:
    video_folder = os.path.join(init_data_path, label)  # Source video folder

    if not os.path.exists(video_folder):
        print(f"Skipping {video_folder}, folder not found.")
        continue

    # Loop through all video files in the folder
    for video_file in os.listdir(video_folder):
        if video_file.endswith((".mp4", ".avi", ".mov")):  # Add more formats if needed
            video_path = os.path.join(video_folder, video_file)

            # Open video
            video = cv2.VideoCapture(video_path)

            frame_count = 0
            saved_frames = 0
            extracted_frames = []  # Store paths for splitting later

            while video.isOpened():
                success, frame = video.read()
                if not success:
                    break

                # Save only every 'frame_skip' frame
                if frame_count % frame_skip == 0:
                    frame_filename = f"{video_file[:-4]}_frame_{saved_frames:04d}.jpg"
                    save_path = os.path.join(train_path, label, frame_filename)
                    cv2.imwrite(save_path, frame)
                    extracted_frames.append(save_path)
                    saved_frames += 1

                frame_count += 1

            video.release()

            # Split into training and validation sets
            random.shuffle(extracted_frames)
            val_size = int(len(extracted_frames) * (1 - train_ratio))  # 20% for validation

            for i in range(val_size):
                val_frame_path = extracted_frames[i]
                new_val_path = val_frame_path.replace("/train/", "/val/")
                shutil.move(val_frame_path, new_val_path)  # Move to validation folder

            print(f"{saved_frames} frames extracted from {video_file}. {val_size} moved to validation set.")

print("\nProcessing complete. Data organized into train/val structure.")
