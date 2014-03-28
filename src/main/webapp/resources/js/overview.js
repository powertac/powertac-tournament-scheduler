function toggleStateViz(brokerId) {
  $("#brokersForm\\:databrokers").find('>tbody>tr').each(function () {
    var id = $(this).find('td:nth-child(1)')[0];
    if ($(id).text() == brokerId) {
      var td = $(this).find('td:nth-child(2)')[0];
      $(td).toggleClass("enabled");
      $(td).toggleClass("disabled");
    }
  });
}

function toggleBrokerViz() {
  var button = $("#brokersForm\\:toggleBrokerViz");

  if (button.val() == "Hide inactive") {
    button.val("Show inactive");

    $("#brokersForm\\:databrokers").find('>tbody>tr').each(function () {
      var sp = $(this).find('td:nth-child(4)')[0];
      var tournaments = $(sp).text();

      if (tournaments.length == 0) {
        $(this).css("display", "none");
      }
    });
  } else {
    button.val("Hide inactive");

    $("#brokersForm\\:databrokers").find(">tbody>tr").each(function () {
      $(this).css("display", "");
    });
  }

  var newHeight = Math.min(400, $("[id$=databrokers]").height()) + "px";
  $('#brokersForm\\:databrokers').parent().height(newHeight);
}

function toggleGamesViz() {
  var active_statuses = ['boot_in_progress', 'game_pending',
    'game_ready', 'game_in_progress', 'boot_failed', 'game_failed'];

  var button = $("#gamesForm\\:toggleGameViz");

  if (button.val() == "Hide inactive") {
    button.val("Show inactive");

    $("#gamesForm\\:dataGames").find('>tbody>tr').each(function () {
      var sp = $(this).find('td:nth-child(3) span')[0];
      var status = $(sp).text();

      if (!($.inArray(status, active_statuses) > -1)) {
        $(this).css("display", "none");
      }
    });
  } else {
    button.val("Hide inactive");

    $("#gamesForm\\:dataGames").find(">tbody>tr").each(function () {
      $(this).css("display", "");
    });
  }

  var newHeight = Math.min(400, $("[id$=dataGames]").height()) + "px";
  $('#gamesForm\\:dataGames').parent().height(newHeight);
}

function updateBrokers(data) {
  $('#brokersForm\\:databrokers').find('>tbody>tr').each(function () {
    var sp = $(this).find('td:first-child span')[0];
    var orgRowNr = $(sp).attr("id").split(":")[2];

    if (data[$(sp).text()] != undefined) {
      $('#brokersForm\\:databrokers\\:' + orgRowNr +
          '\\:checkins').html(data[$(sp).text()]);
    } else {
      $('#brokersForm\\:databrokers\\:' + orgRowNr +
          '\\:checkins').html("");
    }
  });
}

function updateGames(data) {
  var allRows = $('#gamesForm\\:dataGames').find('>tbody>tr');
  allRows.each(function () {
    var sp = $(this).find('td:first-child span')[0];
    var orgRowNr = $(sp).attr("id").split(":")[2];

    var hb = $('#gamesForm\\:dataGames\\:' + orgRowNr + '\\:heartbeat');
    hb.html("");
    hb.attr("title", "");
    var et = $('#gamesForm\\:dataGames\\:' + orgRowNr + '\\:step');
    et.html("");

    if (data[$(sp).text()] != undefined) {
      var heartBeat = data[$(sp).text()].split(";")[0];
      var parts = data[$(sp).text()].split(" ");
      var duration = Math.round((parts[2] - parts[0]) * 5 / 60);
      var elapsedTime = data[$(sp).text()].split(";")[1];

      hb.html(heartBeat);
      if (!isNaN(duration)) {
        hb.attr("title", duration + " minutes");
      }
      if (elapsedTime != undefined) {
        et.html(elapsedTime);
      }
    }
  });

  $('#gamesFormHeader').text('Pending/Running Games (' + allRows.length + ')');
}

function updateTables() {
  $.ajax({
    url: "Rest?type=brokers",
    success: updateBrokers
  });
  $.ajax({
    url: "Rest?type=games",
    success: updateGames
  });
}

function resizeTables() {
  $('[id$=databrokers]').dataTable({
    "bFilter": false,
    "bInfo": false,
    "sScrollY": Math.min(400, $("[id$=databrokers]").height()) + "px",
    "bPaginate": false,
    "aoColumnDefs": [
      { 'bSortable': false, 'aTargets': [3, 4, 5] },
      { "sType": "natural", "aTargets": [0] }
    ]
  });
  $('[id$=dataRounds]').dataTable({
    "bFilter": false,
    "bInfo": false,
    "sScrollY": Math.min(400, $("[id$=dataRounds]").height()) + "px",
    "bPaginate": false,
    "aoColumnDefs": [
      { 'bSortable': false, 'aTargets': [4, 5, 6] },
      { "sType": "natural", "aTargets": [0] }
    ]
  });
  $('[id$=dataGames]').dataTable({
    "bFilter": false,
    "bInfo": false,
    "sScrollY": Math.min(400, $("[id$=dataGames]").height()) + "px",
    "bPaginate": false,
    "aoColumnDefs": [
      { 'bSortable': false, 'aTargets': [3, 4, 5, 6] },
      { "sType": "natural", "aTargets": [0, 1] }
    ]
  });
}

$(document).ready(function () {
  resizeTables();

  if ($('#brokersForm\\:hideInactiveBrokers').val() == 'true') {
    toggleBrokerViz();
  }

  if ($('#gamesForm\\:hideInactiveGames').val() == 'true') {
    toggleGamesViz();
  }

  updateTables();
  setInterval(updateTables, 3000);
});
