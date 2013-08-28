function showGamesCount() {
  var element = document.getElementById('saveRound');
  if (typeof(element) == 'undefined' || element == null) {
    return;
  }

  var maxSlaves = document.getElementById("slavesCount").value;
  var durationOverhead = 1.1;

  var elems = saveRound.elements;
  var gameName = elems[0].value;
  var maxBrokers = parseInt(elems[1].value);
  var maxAgents = parseInt(elems[2].value);
  var gameType1 = parseInt(elems[3].value);
  var multiplier1 = parseInt(elems[4].value);
  var gameType2 = parseInt(elems[5].value);
  var multiplier2 = parseInt(elems[6].value);
  var gameType3 = parseInt(elems[7].value);
  var multiplier3 = parseInt(elems[8].value);

  setText("totalGames", "");
  setText("total1", "");
  setText("total2", "");
  setText("total3", "");

  if (maxBrokers == 0 || maxAgents == 0) {
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
    totalGames1 = multiplier1 * binomial(maxBrokers, gameType1);
    slaves = Math.min(maxSlaves, maxAgents * maxBrokers / gameType1, totalGames1);
    totalTime1 = durationOverhead * gameDuration * (totalGames1 / slaves);
    setText("total1", "Games / duration : " + totalGames1 + " / " + Math.floor(totalTime1 * 10) / 10);
  }
  if ((maxBrokers > 0) && (gameType2 > 0) && (multiplier2 > 0)) {
    if (gameType2 > maxBrokers) {
      elems[6].value = maxBrokers;
      gameType2 = maxBrokers;
    }
    totalGames2 = multiplier2 * binomial(maxBrokers, gameType2);
    slaves = Math.min(maxSlaves, maxAgents * maxBrokers / gameType2, totalGames2);
    totalTime2 = durationOverhead * gameDuration * (totalGames2 / slaves);
    setText("total2", "Games / duration : " + totalGames2 + " / " + Math.floor(totalTime2 * 10) / 10);
  }
  if ((maxBrokers > 0) && (gameType3 > 0) && (multiplier3 > 0)) {
    if (gameType3 > maxBrokers) {
      elems[8].value = maxBrokers;
      gameType3 = maxBrokers;
    }
    totalGames3 = multiplier3 * binomial(maxBrokers, gameType3);
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

function binomial(n, k) {
  if (k == 0) {
    return 1;
  } else if (2 * k > n) {
    return binomial(n, n - k);
  } else {
    e = n - k + 1;
    for (var i = 2; i < k + 1; i++) {
      e *= (n - k + i);
      e /= i;
    }
    return e;
  }
}

$(document).ready(function () {
  showGamesCount();
});