function updateVisualizers(data) {
  $('#machinesForm\\:dataMachines').find('>tbody>tr').each(function () {
    var sp = $(this).find('td:first-child span')[0];
    var orgRowNr = $(sp).attr("id").split(":")[2];

    if (data[$(sp).text()] != undefined) {
      $('#machinesForm\\:dataMachines\\:' + orgRowNr + '\\:checkins').html(data[$(sp).text()]);
    } else {
      $('#machinesForm\\:dataMachines\\:' + orgRowNr + '\\:checkins').html("");
    }
  });
}

function updateScheduler(data) {
  if (data['text'] != undefined) {
    $('#adminControls\\:scheduler').html(data['text']);
  } else {
    $('#adminControls\\:scheduler').html("");
  }
}

function resizeTables() {
  $('[id$=dataPoms]').dataTable({
    "bFilter": false,
    "bInfo": false,

    "bPaginate": false
  });

  $('[id$=dataMachines]').dataTable({
    "bFilter": false,
    "bInfo": false,

    "bPaginate": false,
    "aoColumnDefs": [
      { 'bSortable': false, 'aTargets': [3, 5, 7, 8] },
      { "sType": "natural", "aTargets": [0, 1, 2] }
    ]
  });

  $('[id$=dataUsers]').dataTable({
    "bFilter": false,
    "bInfo": false,

    "bPaginate": false,
    "aoColumnDefs": [
      { 'bSortable': false, 'aTargets': [3] },
    ]
  });
}

function updateTables() {
  $.ajax({
    url: "Rest?type=visualizers",
    success: updateVisualizers
  });
  $.ajax({
    url: "Rest?type=scheduler",
    success: updateScheduler
  });
}

$(document).ready(function () {
  resizeTables();
  updateTables();
  setInterval(updateTables, 3000);
});