function toggleSample() {
    if ($("#toggleSampleButton").val() == "Hide sample") {
        $('#toggleSampleButton').val("Show sample");

        $("#formDatabrokers\\:databrokers").find('>tbody>tr').each(function () {
            var sp = $(this).find('td:nth-child(2) a')[0];
            var name = $(sp).text();

            if (name.toLowerCase().indexOf("sample") > -1) {
                $(this).css("display", "none");
            }
        });

        $('#formDatabrokers\\:hideSample').val('true');
        $('#tournamentForm\\:hideSample').val('true');
        $('#gamesForm\\:hideSample').val('true');
    } else {
        $('#toggleSampleButton').val("Hide sample");

        $("#formDatabrokers\\:databrokers").find(">tbody>tr").each(function () {
            $(this).css("display", "");
        });

        $('#formDatabrokers\\:hideSample').val('false');
        $('#tournamentForm\\:hideSample').val('false');
        $('#gamesForm\\:hideSample').val('false');
    }

    var newHeight = Math.min(400, $("[id$=databrokers]").height()) + "px";
    $('#formDatabrokers\\:databrokers').parent().height( newHeight );
}

function toggleActive() {
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

        $('#formDatabrokers\\:hideInactive').val('true');
        $('#tournamentForm\\:hideInactive').val('true');
        $('#gamesForm\\:hideInactive').val('true');
    } else {
        $('#toggleActiveButton').val("Hide inactive");

        $("#gamesForm\\:dataGames").find(">tbody>tr").each(function () {
            $(this).css("display", "");
        });

        $('#formDatabrokers\\:hideInactive').val('false');
        $('#tournamentForm\\:hideInactive').val('false');
        $('#gamesForm\\:hideInactive').val('false');
    }

    var newHeight = Math.min(400, $("[id$=dataGames]").height()) + "px";
    $('#gamesForm\\:dataGames').parent().height( newHeight );
}

function updateBrokers(data) {
    $('#formDatabrokers\\:databrokers').find('>tbody>tr').each(function () {
        var sp = $(this).find('td:first-child span')[0];
        var orgRowNr = $(sp).attr("id").split(":")[2];

        if (data[$(sp).text()] != undefined) {
            $('#formDatabrokers\\:databrokers\\:' + orgRowNr + '\\:checkins').html(data[$(sp).text()]);
        } else {
            $('#formDatabrokers\\:databrokers\\:' + orgRowNr + '\\:checkins').html("");
        }
    });
}

function updateGames(data) {
    $('#gamesForm\\:dataGames').find('>tbody>tr').each(function () {
        var sp = $(this).find('td:first-child span')[0];
        var orgRowNr = $(sp).attr("id").split(":")[2];

        if (data[$(sp).text()] != undefined) {
            $('#gamesForm\\:dataGames\\:' + orgRowNr + '\\:heartbeat').html(data[$(sp).text()]);
        } else {
            $('#gamesForm\\:dataGames\\:' + orgRowNr + '\\:heartbeat').html("");
        }
    });
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
    $('[id$=dataTournaments]').dataTable({
        "bFilter": false,
        "bInfo": false,
        "sScrollY": Math.min(400, $("[id$=dataTournaments]").height()) + "px",
        "bPaginate": false,
        "aoColumnDefs": [
            { 'bSortable': false, 'aTargets': [5, 6, 7, 8] },
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

    if ($('#formDatabrokers\\:hideSample').val() == 'true') {
        toggleSample();
    }

    if ($('#formDatabrokers\\:hideInactive').val() == 'true') {
        toggleActive();
    }

    updateTables();
    setInterval(updateTables, 3000);
});