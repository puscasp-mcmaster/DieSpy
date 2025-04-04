def main():
    from ultralytics import YOLO
    import torch

    model_path = "runs/detect/4_2_200_s/weights/best_saved_model/best_float32.tflite"

    torch.multiprocessing.freeze_support()
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = YOLO(model=model_path)
    metrics = model.val(data="dataset/testdata.yaml", imgsz = 640, name="m_4_2_200", device=device, plots=True) #conf=float to restrict the conf used for prediction

    metrics.box.maps


if __name__ == '__main__':
    main()
