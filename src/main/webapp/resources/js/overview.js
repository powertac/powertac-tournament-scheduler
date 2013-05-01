function toggleActiveBrokers() {
    if ($("#toggleSampleButton").val() == "Hide inactive") {
        $('#toggleSampleButton').val("Show active");

        $("#brokersForm\\:databrokers").find('>tbody>tr').each(function () {
            var sp = $(this).find('td:nth-child(4)')[0];
            var tournaments = $(sp).text();

            if (tournaments.length == 0) {
                $(this).css("display", "none");
            }
        });

        $('#brokersForm\\:hideSample').val('true');
        $('#roundForm\\:hideSample').val('true');
        $('#gamesForm\\:hideSample').val('true');
    } else {
        $('#toggleSampleButton').val("Hide inactive");

        $("#brokersForm\\:databrokers").find(">tbody>tr").each(function () {
            $(this).css("display", "");
        });

        $('#brokersForm\\:hideSample').val('false');
        $('#roundForm\\:hideSample').val('false');
        $('#gamesForm\\:hideSample').val('false');
    }

    var newHeight = Math.min(400, $("[id$=databrokers]").height()) + "px";
    $('#brokersForm\\:databrokers').parent().height( newHeight );
}

function toggleActiveGames() {
    var active_statuses = new Array('boot_in_progress', 'game_pending', 'game_ready', 'game_in_progress');

    if ($("#toggleActiveButton").val() == "Hide inactive") {
        $('#toggleActiveButton').val("Show inactive");

        $("#gamesForm\\:dataGames").find('>tbody>tr').each(function () {
            var sp = $(this).find('td:nth-child(3) span')[0];
            var status = $(sp).text();

            if (!($.inArray(status, active_statuses) > -1)) {
                $(this).css("display", "none");
            }
        });

        $('#brokersForm\\:hideInactive').val('true');
        $('#roundForm\\:hideInactive').val('true');
        $('#gamesForm\\:hideInactive').val('true');
    } else {
        $('#toggleActiveButton').val("Hide inactive");

        $("#gamesForm\\:dataGames").find(">tbody>tr").each(function () {
            $(this).css("display", "");
        });

        $('#brokersForm\\:hideInactive').val('false');
        $('#roundForm\\:hideInactive').val('false');
        $('#gamesForm\\:hideInactive').val('false');
    }

    var newHeight = Math.min(400, $("[id$=dataGames]").height()) + "px";
    $('#gamesForm\\:dataGames').parent().height( newHeight );
}

function updateBrokers(data) {
    $('#brokersForm\\:databrokers').find('>tbody>tr').each(function () {
        var sp = $(this).find('td:first-child span')[0];
        var orgRowNr = $(sp).attr("id").split(":")[2];

        if (data[$(sp).text()] != undefined) {
            $('#brokersForm\\:databrokers\\:' + orgRowNr + '\\:checkins').html(data[$(sp).text()]);
        } else {
            $('#brokersForm\\:databrokers\\:' + orgRowNr + '\\:checkins').html("");
        }
    });
}

function updateGames(data) {
    var allRows = $('#gamesForm\\:dataGames').find('>tbody>tr');
    allRows.each(function () {
        var sp = $(this).find('td:first-child span')[0];
        var orgRowNr = $(sp).attr("id").split(":")[2];

        var spn = $('#gamesForm\\:dataGames\\:' + orgRowNr + '\\:heartbeat');
        if (data[$(sp).text()] != undefined) {
            var parts = data[$(sp).text()].split(" ");
            var duration = Math.round((parts[2] - parts[0]) * 5 / 60);
            spn.html(data[$(sp).text()]);
            spn.attr("title", duration + " mins");
        } else {
            spn.html("");
            spn.attr("title", "");
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

    if ($('#brokersForm\\:hideSample').val() == 'true') {
        toggleActiveBrokers();
    }

    if ($('#brokersForm\\:hideInactive').val() == 'true') {
        toggleActiveGames();
    }

    updateTables();
    setInterval(updateTables, 3000);
});