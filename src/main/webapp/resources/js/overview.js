function toggleStateViz(brokerId) {
  $.ajax({
    url: "Rest?type=brokerState",
    type: "POST",
    data: {brokerId: brokerId}
  });

  $("#brokersForm\\:databrokers").find('>tbody>tr').each(function () {
    var id = $(this).find('td:nth-child(1)')[0];
    if ($(id).text() == brokerId) {
      var td = $(this).find('td:nth-child(2)')[0];
      $(td).toggleClass("enabled");
      $(td).toggleClass("disabled");
    }
  });
}

function updateBrokerViz () {
  var button = $("#brokersForm\\:toggleBrokerViz");
  var databrokers = $("#brokersForm\\:databrokers");

  if (localStorage["hideInactiveBrokers"] === "true") {
    button.val("Hide inactive");
    databrokers.find(">tbody>tr").each(function () {
      $(this).css("display", "");
    });
  }
  else {
    button.val("Show inactive");
    databrokers.find('>tbody>tr').each(function () {
      var sp = $(this).find('td:nth-child(4)')[0];
      var tournaments = $(sp).text();

      if (tournaments.length == 0) {
        $(this).css("display", "none");
      }
    });
  }
}

function toggleBrokerViz() {
  if (localStorage["hideInactiveBrokers"] === "true") {
    localStorage["hideInactiveBrokers"] = false;
  }
  else {
    localStorage["hideInactiveBrokers"] = true;
  }
  updateBrokerViz();
}

function updateGamesViz () {
  var active_statuses = ['boot_in_progress', 'game_pending',
    'game_ready', 'game_in_progress', 'boot_failed', 'game_failed'];

  var button = $("#gamesForm\\:toggleGameViz");
  var dataGames = $("#gamesForm\\:dataGames");

  if (localStorage["hideInactiveGames"] === "true") {
    button.val("Hide inactive");

    dataGames.find(">tbody>tr").each(function () {
      $(this).css("display", "");
    });
  }
  else {
    button.val("Show inactive");

    dataGames.find('>tbody>tr').each(function () {
      var sp = $(this).find('td:nth-child(3) span')[0];
      var status = $(sp).text();

      if (!($.inArray(status, active_statuses) > -1)) {
        $(this).css("display", "none");
      }
    });
  }
}

function toggleGamesViz() {
  if (localStorage["hideInactiveGames"] === "true") {
    localStorage["hideInactiveGames"] = false;
  }
  else {
    localStorage["hideInactiveGames"] = true;
  }
  updateGamesViz();
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
  var databrokers = $('[id$=databrokers]');
  databrokers.dataTable({
    "bFilter": false,
    "bInfo": false,
    "sScrollY": Math.min(400, databrokers.height()) + "px",
    "bPaginate": false,
    "aoColumnDefs": [
      { 'bSortable': false, 'aTargets': [3, 4, 5] },
      { "sType": "natural-nohtml", "aTargets": [0, 1] }
    ]
  });

  var dataRounds = $('[id$=dataRounds]');
  dataRounds.dataTable({
    "bFilter": false,
    "bInfo": false,
    "sScrollY": Math.min(400, dataRounds.height()) + "px",
    "bPaginate": false,
    "aoColumnDefs": [
      { 'bSortable': false, 'aTargets': [4, 5, 6] },
      { "sType": "natural-nohtml", "aTargets": [0] }
    ]
  });

  var dataGames = $('[id$=dataGames]');
  dataGames.dataTable({
    "bFilter": false,
    "bInfo": false,
    "sScrollY": Math.min(400, dataGames.height()) + "px",
    "bPaginate": false,
    "aoColumnDefs": [
      { 'bSortable': false, 'aTargets': [3, 4, 5, 6, 7] },
      { "sType": "natural-nohtml", "aTargets": [0] }
    ]
  });
}

$(document).ready(function () {
  resizeTables();

  updateBrokerViz();
  updateGamesViz();

  updateTables();
  setInterval(updateTables, 3000);
});
