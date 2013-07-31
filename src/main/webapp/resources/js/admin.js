function updateVisualizers(data) {
    $('#machinesForm\\:machines').find('>tbody>tr').each(function () {
        var sp = $(this).find('td:first-child span')[0];
        var orgRowNr = $(sp).attr("id").split(":")[2];

        if (data[$(sp).text()] != undefined) {
            $('#machinesForm\\:machines\\:' + orgRowNr + '\\:checkins').html(data[$(sp).text()]);
        } else {
            $('#machinesForm\\:machines\\:' + orgRowNr + '\\:checkins').html("");
        }
    });
}

function updateWatchdog(data) {
    if (data['text'] != undefined) {
        $('#adminControls\\:watchdog').html(data['text']);
    } else {
        $('#adminControls\\:watchdog').html("");
    }
}

function resizeTables() {
  $('[id$=dataPoms]').dataTable({
    "bFilter": false,
    "bInfo": false,

    "bPaginate": false,
    /*
    "aoColumnDefs": [
      { 'bSortable': false, 'aTargets': [3, 5, 7, 8] },
    ]
    */
  });

  $('[id$=dataMachines]').dataTable({
    "bFilter": false,
    "bInfo": false,

    "bPaginate": false,
    "aoColumnDefs": [
      { 'bSortable': false, 'aTargets': [3, 5, 7, 8] },
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
        url: "Rest?type=watchdog",
        success: updateWatchdog
    });
}

$(document).ready(function () {
    resizeTables();
    updateTables();
    setInterval(updateTables, 3000);
});