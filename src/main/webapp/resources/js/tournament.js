function updateSummary() {
  var newContent = contentForm.elements["contentForm:contentInput"];
  var summary = document.getElementById('summary');
  summary.innerHTML = newContent.value;
}

function updateButtons(buttonId) {
  window.setTimeout(function (buttonId) {
    var element = document.getElementById(buttonId);
    element.parentNode.removeChild(element);
  }, 100, buttonId);
}