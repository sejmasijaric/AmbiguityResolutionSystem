from ultralytics import YOLO

#load a model
model = YOLO("/Users/sejmasijaric/Documents/Bachelor Thesis/YOLO-model/yolo11n-cls.pt")

train_results = model.train(
    data = "/Users/sejmasijaric/Documents/Bachelor Thesis/HandGestureTrainingDataClassified",
    epochs = 30,
    imgsz = 640,
    device = "cpu",
)

metrics = model.val()
