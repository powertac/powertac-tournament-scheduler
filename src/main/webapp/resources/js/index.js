function resizeTables() {
  $('[id$=runnning_games]').dataTable({
    "bFilter": false,
    "bInfo": false,
    "sScrollY": Math.min(400, $("[id$=runnning_games]").height()) + "px",
    "bPaginate": false,
    "aoColumnDefs": [
      { 'bSortable': false, 'aTargets': [2, 5] },
      { "sType": "natural", "aTargets": [0, 1] }
    ]
  });
  $('[id$=completed_games]').dataTable({
    "bFilter": false,
    "bInfo": false,
    "sScrollY": Math.min(400, $("[id$=completed_games]").height()) + "px",
    "bPaginate": false,
    "aoColumnDefs": [
      { 'bSortable': false, 'aTargets': [2, 4] },
      { "sType": "natural", "aTargets": [0, 1] }
    ]
  });
}

$(document).ready(function () {
  resizeTables();
});
