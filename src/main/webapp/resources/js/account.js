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

$(document).ready(function () {
    updateTables();
    setInterval(updateTables, 3000);
});