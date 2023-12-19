function onClickedPredictDisease() {
    var fileInput = document.getElementById("uiImageUpload");
    var imageFile = fileInput.files[0];
    var formData = new FormData();
    formData.append("file", imageFile);

    //var url = "http://localhost:8000/predict";
    var url = "/api/predict";
    $.ajax({
        url: url,
        type: "POST",
        data: formData,
        processData: false,
        contentType: false,
        success: function (data) {
            console.log(data);
            var predictedClass = data.class;
            var confidence = data.confidence;
            var diseaseResult = document.getElementById("uiDiseaseResult");
            diseaseResult.innerHTML = "<h2>Class: " + predictedClass + "</h2><h2>Confidence: " + confidence + "</h2>";
        },
        error: function (error) {
            console.log(error);
        }
    });
}
