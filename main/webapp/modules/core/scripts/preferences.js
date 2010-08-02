var preferenceUIs = [];

function onLoad() {
    $.post(
        "/command/get-all-preferences",
        null,
        populatePreferences,
        "json"
    );
}

$(onLoad);

function populatePreferences(prefs) {
    var body = $("#body").empty();
    
    $('<h1>').text("Preferences").appendTo(body);
    
    var table = $('<table>').addClass("preferences").html('<tr><th>Key</th><th>Value</th><td></td></tr>').appendTo(body)[0];
    for (var k in prefs) {
        var tr = table.insertRow(table.rows.length);
        preferenceUIs.push(new PreferenceUI(tr, k, prefs[k]));
    }
    
    var trLast = table.insertRow(table.rows.length);
    var tdLast0 = trLast.insertCell(0);
    $('<button>').text("Add Preference").appendTo(tdLast0).click(function() {
        var key = window.prompt("Preference Key");
        if (key) {
            var value = window.prompt("Preference Value");
            if (value != null) {
                var tr = table.insertRow(table.rows.length - 1);
                preferenceUIs.push(new PreferenceUI(tr, key, value));
                
                $.post(
                    "/command/set-preference",
                    {
                        name : key,
                        value : JSON.stringify(value)
                    },
                    function(o) {
                        if (o.code == "error") {
                            alert(o.message);
                        }
                    },
                    "json"
                );
            }
        }
    });
}

function PreferenceUI(tr, key, value) {
    var self = this;
    
    var td0 = tr.insertCell(0);
    $(td0).text(key);
    
    var td1 = tr.insertCell(1);
    $(td1).text(value != null ? value : "");
    
    var td2 = tr.insertCell(2);
    
    $('<button>').text("Edit").appendTo(td2).click(function() {
        var newValue = window.prompt("Value of preference key " + key, value);
        if (newValue != null) {
            $(td1).text(newValue);
            $.post(
                "/command/set-preference",
                {
                    name : key,
                    value : JSON.stringify(newValue)
                },
                function(o) {
                    if (o.code == "error") {
                        alert(o.message);
                    }
                },
                "json"
            );
        }
    });
    
    $('<button>').text("Delete").appendTo(td2).click(function() {
        if (window.confirm("Delete preference key " + key + "?")) {
            $.post(
                "/command/set-preference",
                {
                    name : key
                },
                function(o) {
                    if (o.code == "ok") {
                        $(tr).remove();
                    
                        for (var i = 0; i < preferenceUIs.length; i++) {
                            if (preferenceUIs[i] === self) {
                                preferenceUIs.splice(i, 1);
                                break;
                            }
                        }
                    } else if (o.code == "error") {
                        alert(o.message);
                    }
                },
                "json"
            );
        }
    });
}