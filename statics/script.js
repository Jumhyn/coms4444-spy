// ctx: drawing board.
// skill: list of skills of players.
// type: home/away
// name: group #
// text_pos: -1/+1 - text is above/below.
// x_start: x starting position of the drawing.
// y_start: y starting position of the drawing.
function drawPlayers(ctx, x_start, y_start, skills, type, name, score, text_pos, fill, isWin) {
    var x_off = 10;
    var y_off = 20;

    ctx.font = '20px Arial';
    ctx.fillStyle = 'black';

    for (var i=0; i<skills.length; ++i) {
        ctx.beginPath();
        ctx.arc(x_start + 5 + x_off, y_start-7, 5, 0*Math.PI, 2*Math.PI);

        if (fill[i]) {
            ctx.fill();
        }

        ctx.stroke();
        ctx.fillText(skills[i], x_start + x_off - 2, y_start + text_pos*y_off);
    
        x_off += 40;
    }

    if (isWin) {
        ctx.font = 'bold 20px Arial';
    }

    ctx.fillText(type + '(' + name + '): ' + score, x_start + x_off, y_start);
    ctx.stroke();
}

function drawGrid(ctx)
{
    ctx.beginPath();
    ctx.lineWidth="0.5 ";
    ctx.strokeStyle="black";
    for (var i = 0; i < 101; i++)
    {
        drawLine(ctx, 0, i * 10, 1000, i * 10);
        drawLine(ctx, i * 10, 0, i * 10, 1000);
    }
}

function drawLine(ctx, x_start, y_start, x_end, y_end) {
    ctx.beginPath();
    ctx.moveTo(x_start, y_start);
    ctx.lineTo(x_end, y_end);
    ctx.stroke();
}

function drawPlayers(ctx, players)
{
    var colors = ["aqua", "chartreuse", "blue", "darkblue", "darkgreen", "darkmagenta", "salmon", "gold", "deeppink"]
    for (var i = 0; i < players.length; i++)
    {
        var p = players[i];
        
        if (p.spy)
        {
            ctx.fillStyle="red";
        }
        else
        {
            ctx.fillStyle=colors[i % 9];
        }
        
        ctx.beginPath();
        ctx.arc(p.x * 10 + 5, p.y * 10 + 5, 4, 0, 2*Math.PI);
        ctx.arc(1030, 50 * i + 20, 10, 0, 2*Math.PI);
        ctx.font = "12px Arial";
        ctx.fillText(p.name[1], p.x * 10 + 12, p.y * 10 + 12);
        ctx.fill();
        
        ctx.font = "30px Arial";
        ctx.fillText(p.name + (p.spy ? " (SPY)" : ""),1060,50*i + 30);
    }
}

function drawRects(ctx, coords, color)
{
    for (var i = 0; i < coords.length; i++)
    {
        var c = coords[i];
        ctx.fillStyle = color;
        
        ctx.beginPath();
        ctx.rect(c.x * 10, c.y * 10, 10, 10);
        ctx.fill();
    }
}

function drawPackage(ctx, point)
{
    ctx.fillStyle = "black";
    ctx.beginPath();
    ctx.rect(point.x * 10 + 2.5, point.y * 10 + 2.5, 5, 5);
    ctx.fill();
}

function drawTarget(ctx, point)
{
    ctx.strokeStyle = "black";
    drawLine(ctx, point.x * 10, point.y * 10, point.x * 10 + 10, point.y * 10 + 10);
    drawLine(ctx, point.x * 10, point.y * 10 + 10, point.x * 10 + 10, point.y * 10);
}

function drawPath(ctx, path, color)
{
    ctx.strokeStyle = color;
    ctx.lineWidth = 2;
    ctx.beginPath();
    for (var i = 1; i < path.length; i++)
    {
        ctx.moveTo(path[i - 1].x * 10 + 5, path[i - 1].y * 10 + 5);
        ctx.lineTo(path[i].x * 10 + 5, path[i].y * 10 + 5);
    }
    ctx.stroke();
}

function process(data) {
    console.log(data);
    var result = JSON.parse(data)

    console.log(result);
    var refresh = parseFloat(result.refresh);
    var t = result.t;
    var elapsed = result.elapsed;
    
    canvas = document.getElementById('canvas');
    ctx = canvas.getContext('2d');
    
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    
    drawRects(ctx, result.water, "blue");
    drawRects(ctx, result.mud, "brown");
    
    drawPackage(ctx, result.package);
    drawTarget(ctx, result.target);
    
    drawPlayers(ctx, result.players);
    drawGrid(ctx);
    
    if (result.final_path != -1)
    {
        drawPath(ctx, result.final_path, (result.victory ? "green" : "red"));
        timeElement = document.getElementById('time');
        timeElement.innerHTML = "Score: " + (result.victory ? elapsed : (2 * t));
    }
    else if (elapsed == t)
    {
        timeElement = document.getElementById('time');
        timeElement.innerHTML = "Score: " + elapsed;
    }
    else
    {
        timeElement = document.getElementById('time');
        timeElement.innerHTML = "Time: " + elapsed + "/" + t;
    }

    return refresh;
}

var latest_version = -1;

function ajax(version, retries, timeout) {
    console.log("Version " + version);
    var xhttp = new XMLHttpRequest();
    xhttp.onload = (function() {
            var refresh = -1;
            try {
                if (xhttp.readyState != 4)
                    throw "Incomplete HTTP request: " + xhttp.readyState;
                if (xhttp.status != 200)
                    throw "Invalid HTTP status: " + xhttp.status;
                //console.log(xhttp.responseText);
                refresh = process(xhttp.responseText);
                if (latest_version < version)
                    latest_version = version;
                else refresh = -1;
            } catch (message) {
                alert(message);
            }

            console.log(refresh);
            if (refresh >= 0)
                setTimeout(function() { ajax(version + 1, 10, 100); }, refresh);
        });
    xhttp.onabort = (function() { location.reload(true); });
    xhttp.onerror = (function() { location.reload(true); });
    xhttp.ontimeout = (function() {
            if (version <= latest_version)
                console.log("AJAX timeout (version " + version + " <= " + latest_version + ")");
            else if (retries == 0)
                location.reload(true);
            else {
                console.log("AJAX timeout (version " + version + ", retries: " + retries + ")");
                ajax(version, retries - 1, timeout * 2);
            }
        });
    xhttp.open("GET", "data.txt", true);
    xhttp.responseType = "text";
    xhttp.timeout = timeout;
    xhttp.send();
}

ajax(1, 10, 100);

