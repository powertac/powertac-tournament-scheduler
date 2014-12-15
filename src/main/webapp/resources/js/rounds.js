function createParamStrings() {
  var paramString =
      document.getElementById("saveRound:maxBrokers").value + "_" +
      document.getElementById("saveRound:maxAgents").value + "_" +
      document.getElementById("saveRound:size1").value + "_" +
      document.getElementById("saveRound:multiplier1").value + "_" +
      document.getElementById("saveRound:size2").value + "_" +
      document.getElementById("saveRound:multiplier2").value + "_" +
      document.getElementById("saveRound:size3").value + "_" +
      document.getElementById("saveRound:multiplier3").value;

  var dateString =
      document.getElementById("startTime.year").value + "-" +
      document.getElementById("startTime.month").value + "-" +
      document.getElementById("startTime.day").value + " " +
      document.getElementById("startTime.hours").value + ":" +
      document.getElementById("startTime.minutes").value;

  var nameString = document.getElementById("saveRound:roundNameID").value;

  document.getElementById("forecaster:paramString").value = paramString;
  document.getElementById("forecaster:dateString").value = dateString;
  document.getElementById("forecaster:nameString").value = nameString;

  return true;
}

function showGamesCount() {
  var element = document.getElementById('saveRound');
  if (typeof(element) == 'undefined' || element == null) {
    return;
  }

  var maxSlaves = document.getElementById("slavesCount").value;
  var durationOverhead = 1.1;

  var gameName = document.getElementById("saveRound:roundNameID").value;
  var maxBrokers = parseInt(document.getElementById("saveRound:maxBrokers").value);
  var maxAgents = parseInt(document.getElementById("saveRound:maxAgents").value);
  var size1 = parseInt(document.getElementById("saveRound:size1").value);
  var multiplier1 = parseInt(document.getElementById("saveRound:multiplier1").value);
  var size2 = parseInt(document.getElementById("saveRound:size2").value);
  var multiplier2 = parseInt(document.getElementById("saveRound:multiplier2").value);
  var size3 = parseInt(document.getElementById("saveRound:size3").value);
  var multiplier3 = parseInt(document.getElementById("saveRound:multiplier3").value);

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
  if ((maxBrokers > 0) && (size1 > 0) && (multiplier1 > 0)) {
    if (size1 > maxBrokers) {
      document.getElementById("saveRound:size1").value = maxBrokers;
      size1 = maxBrokers;
    }
    totalGames1 = multiplier1 * binomial(maxBrokers, size1);
    slaves = Math.min(maxSlaves, maxAgents * maxBrokers / size1, totalGames1);
    totalTime1 = durationOverhead * gameDuration * (totalGames1 / slaves);
    setText("total1", "Games / duration : " + totalGames1 + " / " + Math.floor(totalTime1 * 10) / 10);
  }
  if ((maxBrokers > 0) && (size2 > 0) && (multiplier2 > 0)) {
    if (size2 > maxBrokers) {
      document.getElementById("saveRound:size2").value = maxBrokers;
      size2 = maxBrokers;
    }
    totalGames2 = multiplier2 * binomial(maxBrokers, size2);
    slaves = Math.min(maxSlaves, maxAgents * maxBrokers / size2, totalGames2);
    totalTime2 = durationOverhead * gameDuration * (totalGames2 / slaves);
    setText("total2", "Games / duration : " + totalGames2 + " / " + Math.floor(totalTime2 * 10) / 10);
  }
  if ((maxBrokers > 0) && (size3 > 0) && (multiplier3 > 0)) {
    if (size3 > maxBrokers) {
      document.getElementById("saveRound:size3").value = maxBrokers;
      size3 = maxBrokers;
    }
    totalGames3 = multiplier3 * binomial(maxBrokers, size3);
    slaves = Math.min(maxSlaves, maxAgents * maxBrokers / size3, totalGames3);
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
    var e = n - k + 1;
    for (var i = 2; i < k + 1; i++) {
      e *= (n - k + i);
      e /= i;
    }
    return e;
  }
}

function toggleRoundName() {
  var formElement = document.getElementById('saveRound');
  if (typeof(formElement) == 'undefined' || formElement == null) {
    return;
  }
  var checkBoxElement = document.getElementById('saveRound:changeAllRounds');
  if (typeof(checkBoxElement) == 'undefined' || checkBoxElement == null) {
    return;
  }

  var changeAllRoundsCheckbox = checkBoxElement.checked;
  var roundNameID = document.getElementById('saveRound:roundNameID');

  if (changeAllRoundsCheckbox) {
    // disable writing the roundname
    roundNameID.readOnly = true;
  }
  else {
    // enable writing the roundname
    roundNameID.readOnly = false;
  }
}

$(document).ready(function () {
  showGamesCount();
  toggleRoundName();
});
