def main():
    from ultralytics import YOLO
    import torch

    model_path = "yolov10s.pt"
    dataset_yaml_path = "dataset/dataset.yaml"
    name = "4_2_200_s"

    torch.multiprocessing.freeze_support()
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = YOLO(model_path)
    results = model.train(data=dataset_yaml_path, epochs=200, imgsz=640, name=name, device=device, batch=-1,
                          fliplr=0.0, cache=True, deterministic=False, erasing=0.0) 

if __name__ == '__main__':
    main()