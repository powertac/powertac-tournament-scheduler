function showGamesCount() {
    var maxSlaves = document.getElementById("slavesCount").value;
    var durationOverhead = 1.1;

    var elems = saveRound.elements;
    var gameName = elems[0].value;
    var type = elems[1].value;
    var maxBrokers = parseInt(elems[2].value);
    var maxAgents = parseInt(elems[3].value);
    var gameType1 = parseInt(elems[4].value);
    var multiplier1 = parseInt(elems[5].value);
    var gameType2 = parseInt(elems[6].value);
    var multiplier2 = parseInt(elems[7].value);
    var gameType3 = parseInt(elems[8].value);
    var multiplier3 = parseInt(elems[9].value);

    setText("totalGames", "");
    setText("total1", "");
    setText("total2", "");
    setText("total3", "");

    if (type == 'SINGLE_GAME') {
        return;
    }

    var gameDuration = (gameName.toLowerCase().indexOf("test") > -1) ? 0.4 : 2;
    var totalGames1 = 0;
    var totalGames2 = 0;
    var totalGames3 = 0;
    var totalTime1 = 0;
    var totalTime2 = 0;
    var totalTime3 = 0;
    var slaves = 0;
    if ((maxBrokers > 0) && (gameType1 > 0) && (multiplier1 > 0)) {
        if (gameType1 > maxBrokers) {
            elems[4].value = maxBrokers;
            gameType1 = maxBrokers;
        }
        recSafety = 0;
        totalGames1 = calculateGames(maxBrokers, gameType1, multiplier1);
        slaves = Math.min(maxSlaves, maxAgents * maxBrokers / gameType1, totalGames1);
        totalTime1 = durationOverhead * gameDuration * (totalGames1 / slaves);
        setText("total1", "Games / duration : " + totalGames1 + " / " + Math.floor(totalTime1 * 10) / 10);
    }
    if ((maxBrokers > 0) && (gameType2 > 0) && (multiplier2 > 0)) {
        if (gameType2 > maxBrokers) {
            elems[6].value = maxBrokers;
            gameType2 = maxBrokers;
        }
        recSafety = 0;
        totalGames2 = calculateGames(maxBrokers, gameType2, multiplier2);
        slaves = Math.min(maxSlaves, maxAgents * maxBrokers / gameType2, totalGames2);
        totalTime2 = durationOverhead * gameDuration * (totalGames2 / slaves);
        setText("total2", "Games / duration : " + totalGames2 + " / " + Math.floor(totalTime2 * 10) / 10);
    }
    if ((maxBrokers > 0) && (gameType3 > 0) && (multiplier3 > 0)) {
        if (gameType3 > maxBrokers) {
            elems[8].value = maxBrokers;
            gameType3 = maxBrokers;
        }
        recSafety = 0;
        totalGames3 = calculateGames(maxBrokers, gameType3, multiplier3);
        slaves = Math.min(maxSlaves, maxAgents * maxBrokers / gameType3, totalGames3);
        totalTime3 = durationOverhead * gameDuration * (totalGames3 / slaves);
        setText("total3", "Games / duration : " + totalGames3 + " / " + Math.floor(totalTime3 * 10) / 10);
    }

    var total = totalGames1 + totalGames2 + totalGames3;
    var duration = Math.floor((totalTime1 + totalTime2 + totalTime3) * 10) / 10;
    if (isNaN(total)) {
        setText("totalGames", "To many games (> 2500) to estimate duration. Try decreasing the number of games.");
    }
    else if (total > 0) {
        setText("totalGames", "Total games / estimated duration : " + total + " / " + duration + " hours");
    }
}

function setText(fieldId, newText) {
    var el = document.getElementById(fieldId);
    while (el.childNodes.length >= 1) {
        el.removeChild(el.firstChild);
    }
    el.appendChild(el.ownerDocument.createTextNode(newText));
}

var recSafety = 0;
function calculateGames(players, gametype, multiplier) {
    if (players == gametype) {
        return multiplier;
    }
    if (gametype == 1) {
        return players * multiplier;
    }

    // Poor mans recursion validation
    if (recSafety++ > 2500) {
        return Number.NaN;
    }

    return calculateGames(players - 1, gametype, multiplier) + calculateGames(players - 1, gametype - 1, multiplier);
}

function typeSelected() {
    var elems = saveRound.elements;
    var type = elems[1].value;
    var table = document.getElementById("saveRound:roundTable");

    if (type == 'MULTI_GAME') {
        table.rows[3].style.display = "";
        table.rows[4].style.display = "";
        table.rows[5].style.display = "";
    } else if (type == 'SINGLE_GAME') {
        table.rows[3].style.display = "none";
        table.rows[4].style.display = "none";
        table.rows[5].style.display = "none";
    }

    showGamesCount();
}

$(document).ready(function () {
    typeSelected();
});