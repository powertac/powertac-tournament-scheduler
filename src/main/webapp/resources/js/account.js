function updateBrokers(data) {
  $('#accountForm0\\:brokers').find('>tbody>tr').each(function () {
    var sp = $(this).find('td:first-child span')[0];
    var orgRowNr = $(sp).attr("id").split(":")[2];

    if (data[$(sp).text()] != undefined) {
      $('#accountForm0\\:brokers\\:' + orgRowNr + '\\:checkins').html(data[$(sp).text()]);
    } else {
      $('#accountForm0\\:brokers\\:' + orgRowNr + '\\:checkins').html("");
    }
  });
}

function updateTables() {
  $.ajax({
    url: "Rest?type=brokers",
    success: updateBrokers
  });
}

function randomAuth() {
  var md5 = '';
  var chars = '0123456789abcdef';
  for (var i = 0; i < 32; i++) {
    md5 += chars[Math.round(Math.random() * (chars.length - 1))];
  }

  var element = document.getElementById("randomAuthSpan");
  while (element.childNodes.length >= 1) {
    element.removeChild(element.firstChild);
  }
  element.appendChild(element.ownerDocument.createTextNode(md5));
}

$(document).ready(function () {
  updateTables();
  setInterval(updateTables, 3000);
});