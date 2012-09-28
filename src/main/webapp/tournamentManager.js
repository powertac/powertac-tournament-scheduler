/**
 * Created with IntelliJ IDEA.
 * User: Govert Buijs
 * Date: 8/3/12
 * Time: 4:10 PM
 * To change this template use File | Settings | File Templates.
 */

function showGamesCount() {
    var elems = saveTournament.elements;
    var type = elems[1].value;
    var maxBrokers = parseInt(elems[2].value);
    var gameType1 = parseInt(elems[4].value);
    var gameType2 = parseInt(elems[6].value);
    var gameType3 = parseInt(elems[8].value);
    var multiplier1 = parseInt(elems[5].value);
    var multiplier2 = parseInt(elems[7].value);
    var multiplier3 = parseInt(elems[9].value);

    setText("totalGames", "");
    setText("total1", "");
    setText("total2", "");
    setText("total3", "");

    if (type == 'SINGLE_GAME') {
        return;
    }

    var total1 = 0;
    var total2 = 0;
    var total3 = 0;
    if ((maxBrokers > 0) && (gameType1 > 0) && (multiplier1 > 0)) {
        if (gameType1 > maxBrokers) {
            elems[4].value = maxBrokers;
            gameType1 = maxBrokers;
        }
        total1 = multiplier1 * calculateGames(maxBrokers, gameType1);
        setText("total1", "Games for this type : " + total1);
    }
    if ((maxBrokers > 0) && (gameType2 > 0) && (multiplier2 > 0)) {
        if (gameType2 > maxBrokers) {
            elems[6].value = maxBrokers;
            gameType2 = maxBrokers;
        }
        total2 = multiplier2 * calculateGames(maxBrokers, gameType2);
        setText("total2", "Games for this type : " + total2);
    }
    if ((maxBrokers > 0) && (gameType3 > 0) && (multiplier3 > 0)) {
        if (gameType3 > maxBrokers) {
            elems[8].value = maxBrokers;
            gameType3 = maxBrokers;
        }
        total3 = multiplier3 * calculateGames(maxBrokers, gameType3);
        setText("total3", "Games for this type : " + total3);
    }

    var total = total1 + total2 + total3;
    if (total > 0) {
        setText("totalGames", "Total games to be created : " + total);
    }
}

function setText(fieldId, newText) {
    var el = document.getElementById(fieldId);
    while(el.childNodes.length >= 1) {
        el.removeChild(el.firstChild);
    }
    el.appendChild(el.ownerDocument.createTextNode(newText));
}

function calculateGames(players, gametype) {
    if (players == gametype) {
        return 1;
    }
    if (gametype == 1) {
        return players;
    }
    return calculateGames(players-1, gametype) + calculateGames(players-1, gametype-1);
}

function typeSelected() {
    var elems = saveTournament.elements;
    var type = elems[1].value;
    var table = document.getElementById("saveTournament:tournamentTable");

    if (type == 'MULTI_GAME') {
        table.rows[3].style.display = "";
        table.rows[4].style.display = "";
        table.rows[5].style.display = "";
        table.rows[6].style.display = "";
        table.rows[7].style.display = "";
        table.rows[8].style.display = "";
        table.rows[9].style.display = "";
    } else if (type == 'SINGLE_GAME') {
        table.rows[3].style.display = "none";
        table.rows[4].style.display = "none";
        table.rows[5].style.display = "none";
        table.rows[6].style.display = "none";
        table.rows[7].style.display = "none";
        table.rows[8].style.display = "none";
        table.rows[9].style.display = "none";
    }

    showGamesCount();
}

window.onload = function ()
{
    typeSelected();
};