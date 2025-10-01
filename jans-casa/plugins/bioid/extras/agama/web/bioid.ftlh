<!DOCTYPE html>
<html>

<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width" />
    <title>BWS Unified User Interface</title>
    <link href="./css/uui.css" rel="stylesheet" />
</head>

<body>
    <header class="header">
        <img class="logo" src="./images/logo.svg" alt="BioID Logo" />
        <div id="uuititle" class="title" data-res="titleVerification"></div>
    </header>
    <main class="main">
        <div class="abortbutton">
            <a id="uuicancel" href="" style="text-decoration:none;"><img src="./images/back.svg" class="button-back"
                    alt="abort" data-res="buttonCancel" /><span class="action-title ml-3"
                    data-res="buttonCancel">back</span></a>
        </div>
        <section id="uuiintroduction" class="hidden">
            <div class="intro-title mb-3" data-res="introductionTitle">How it works</div>
            <div class="row">
                <div class="col col-right-border">
                    <div class="col-fix-size float-right">
                        <div class="intro-subtitle" data-res="introductionStep1">Step 1</div>
                        <div class="intro-description" data-res="introductionStep1Desc1">Before you start</div>
                        <div class="intro-description" data-res="introductionStep1Desc2">
                            come closer to the camera
                        </div>
                        <div class="row">
                            <div class="col m-3">
                                <div class="center">
                                    <div class="red center" data-res="introductionTooFarAway">Too far away!</div>
                                    <img src="./images/toofaraway.png" />
                                </div>
                            </div>
                            <div class="col m-3">
                                <div class="center">
                                    <div class="red center" data-res="introductionTooClose">Too close!</div>
                                    <img src="./images/tooclose.png" />
                                </div>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col m-3">
                                <div class="center">
                                    <div class="blue center" data-res="introductionPerfect">Perfect!</div>
                                    <img src="./images/perfect.png" />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col">
                    <div class="col-fix-size float-left">
                        <div class="intro-subtitle" data-res="introductionStep2">Step 2</div>
                        <div class="intro-description" data-res="introductionStep2Desc1">For Liveness Detection</div>
                        <div class="intro-description" data-res="introductionStep2Desc2">
                            nod your head slightly
                        </div>
                        <div class="row mt-3">
                            <div class="col">
                                <div class="center m-3">
                                    <video src="./video/nodyourhead.mp4" class="demo-video" autoplay playsinline loop
                                        muted oncontextmenu="return false;"></video>
                                </div>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col mt-3">
                                <div class="intro-attention blue" data-res="introductionDontMoveDevice"><strong>Please
                                        don't move the device!</strong></div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="row mt-3 mb-3">
                <div class="col">
                    <div class="center">
                        <label class="checkbox">
                            <input type="checkbox" class="intro-checkbox" id="introskip" value="yes"><span
                                data-res="introductionDontShowAgain">GOT IT! Don't show the instruction again</span>
                        </label>
                    </div>
                </div>
            </div>
            <div class="row center m-3">
                <div class="col mb-5">
                    <button onclick="showIntroduction(false)" class="button button-accept"
                        data-res="buttonReadyToStart">I'm ready to start</button>
                </div>
            </div>
        </section>
        <section id="uuiwebapp" class="hidden">
            <div class="row">
                <div class="col center">
                    <div id="uuiinstruction" class="instruction transparent-background"></div>
                </div>
            </div>
            <div class="row mt-3">
                <div class="col center">
                    <button id="uuistart" class="button" data-res="buttonStart">START</button>
                </div>
            </div>
            <div class="row">
                <div class="col center m-3">
                    <div id="uuiliveview" class="liveview">
                        <div id="uuihead" class="head"></div>
                        <canvas id="uuicanvas" class="canvasview"></canvas>
                    </div>
                    <video id="livevideo" class="live" autoplay playsinline oncontextmenu="return false;"></video>
                </div>
            </div>
            <div class="row">
                <div class="col center">
                    <div id="uuistatus" class="modal">
                        <div id="uuimessage"></div>
                        <div class="modal-footer">
                            <button id="uuiok" class="button button-ok">OK</button>
                        </div>
                    </div>
                </div>
            </div>
            <div id="uuisingleupload" class="uploadstatus-single">
                <!-- status images for up to 4 recordings -->
                <div id="uuiimage1" class="image">
                    <div id="uuiwait1" class="spinner spinner-wait"></div>
                    <div id="uuiupload1" class="spinner spinner-upload" data-res="uploadInfo">Uploading...</div>
                    <img id="uuiuploaded1" class="image-uploaded" />
                    <div id="uuiprogress1" class="progress-single">
                        <div id="uuiprogressbar1" class="progressbar"></div>
                    </div>
                </div>
                <div id="uuiimage2" class="image">
                    <div id="uuiwait2" class="spinner spinner-wait"></div>
                    <div id="uuiupload2" class="spinner spinner-upload" data-res="uploadInfo">Uploading...</div>
                    <img id="uuiuploaded2" class="image-uploaded" />
                    <div id="uuiprogress2" class="progress-single">
                        <div id="uuiprogressbar2" class="progressbar"></div>
                    </div>
                </div>
                <div id="uuiimage3" class="image">
                    <div id="uuiwait3" class="spinner spinner-wait"></div>
                    <div id="uuiupload3" class="spinner spinner-upload" data-res="uploadInfo">Uploading...</div>
                    <img id="uuiuploaded3" class="image-uploaded" />
                    <div id="uuiprogress3" class="progress-single">
                        <div id="uuiprogressbar3" class="progressbar"></div>
                    </div>
                </div>
                <div id="uuiimage4" class="image">
                    <div id="uuiwait4" class="spinner spinner-wait"></div>
                    <div id="uuiupload4" class="spinner spinner-upload" data-res="uploadInfo">Uploading...</div>
                    <img id="uuiuploaded4" class="image-uploaded" />
                    <div id="uuiprogress4" class="progress-single">
                        <div id="uuiprogressbar4" class="progressbar"></div>
                    </div>
                </div>
            </div>
            <div id="uuicompactupload" class="uploadstatus-compact">
                <div id="uuiprogresscompact" class="progress-compact">
                    <div id="uuiprogressbarcompact" class="progressbar"></div>
                </div>
            </div>
        </section>
        <section id="uuisplash" class="hidden">
            <div class="row mt-9 d-flex justify-content-center">
                <div class="col-8">
                    <div id="uuiprompt" class="promt">
                        <p data-res="prompt" class="mt-9"></p>
                        <p id="uuierror" class="alert-danger"></p>
                        <a id="uuiskip" class="button button-info" href="" data-res="buttonContinue">Skip biometrics</a>
                        <a id="uuimobileapp" class="button button-info" href="" data-res="buttonMobileApp">Start BioID
                            app</a>
                    </div>
                </div>
            </div>
        </section>
    </main>

    <script src="./js/jquery-3.7.1.min.js"></script>
    <script src="./js/getUserMedia.min.js"></script>
    <script src="./js/bws.capture.js"></script>
    <script src="./js/three.min.js"></script>
    <script src="./js/objLoader.min.js"></script>

    <script type="text/javascript">
        // BEGIN OF CONFIGURATION
        var token = `${token}`;
        var returnURL = "${return_url!"abcdef"}";
        var state = "${state!"abcdef"}";
        var apiurl = "https://bws.bioid.com/extension/";
        var decodedToken = atob(token.split('.')[1]);
        var parsedToken = JSON.parse(decodedToken);
        var task = "verification";
        var trait = "Face,Periocular";
        var executions = 3;
        var recordings = 0;
        var threshold = 25;
        var challengeResponse = false;
        var challenges = "[]";
        var autoenroll = false;
        if (parsedToken.task !== undefined) {
            if ((parsedToken.task & 0x100) == 0x100) {
                recordings = 2;
            }
            if ((parsedToken.task & 0x10) == 0x10) {
                task = "identification";
            } else if ((parsedToken.task & 0x80) == 0x80) {
                task = "livenessdetection";
            } else if ((parsedToken.task & 0x20) == 0x20) {
                task = "enrollment";
                recordings = 3;
            } else if ((parsedToken.task & 0x1000) == 0x1000) {
                autoenroll = true;
            }
        }
        var skipIntro = false;
        // END OF CONFIGURATION


        // BWS capture jQuery plugin
        var bwsCapture = null;

        // counter for current execution
        var currentExecution = 0;

        var currentTag = '';
        var parentTag = '';

        // enrollment without challenge response
        var enrollmentTags = ['left', 'right', 'right', 'left', 'up', 'down', 'down', 'up'];
        var predefinedTags = ['left', 'right', 'up', 'down'];

        // map for the compact upload
        var progressMap = new Map();

        // localized messages (english defaults, might get overloaded in initialize())
        var localizedData = {
            "titleEnrollment": "Enrollment",
            "titleVerification": "Verification",
            "titleIdentification": "Identification",
            "titleLiveDetection": "Liveness Detection",

            "introductionTitle": "How it works",
            "introductionStep1": "Step 1",
            "introductionStep1Desc1": "Before you start",
            "introductionStep1Desc2": "come closer to the camera",
            "introductionTooFarAway": "Too far away!",
            "introductionTooClose": "Too close!",
            "introductionPerfect": "Perfect!",
            "introductionStep2": "Step 2",
            "introductionStep2Desc1": "For Liveness Detection",
            "introductionStep2Desc2": "nod your head slightly",
            "introductionDontMoveDevice": "Please don't move the device!",
            "introductionDontShowAgain": "GOT IT! Don't show the instruction again",

            "buttonCancel": "back",
            "buttonCancel.title": "Abort and navigate back to caller",
            "buttonReadyToStart": "I'm ready to start",
            "buttonContinue": "Skip biometrics",
            "buttonMobileApp": "Start BioID app",

            "prompt": "It seems like we don't have access to the camera. For performing biometric operations, please try the following:<br/>- Grant access to the camera.<br/>- Use a different browser (e.g. the most recent versions of Opera, Firefox, Chrome or Edge).",
            "mobileapp": "If you have installed the BioID App on your mobile device, you can use this app for enrollment or verification.",

            "uploadInfo": "Uploading...",

            "capture-error": "We could not capture an image.<br />Sorry, but without access to the camera, facial recognition and liveness detection aren't possible!",
            "nogetUserMedia": "Your browser does not support the HTML5 Media Capture and Streams API. Please use a different browser or the BioID mobile app.",
            "permissionDenied": "Permission Denied!",
            "webgl-error": "WebGL is disabled or unavailable. If possible activate WebGL or use another browser.",

            "UserInstruction-CloseUp": "Come close before you start",
            "UserInstruction-NodYourHead": "Please nod your head",
            "UserInstruction-FollowMe": "Follow the blue head",
            "UserInstruction-NoMovement": "Follow the head's movement",
            "UserInstruction-PleaseWait": "Please wait",

            "Perform-enrollment": "Training...",
            "Perform-verification": "Verifying...",
            "Perform-identification": "Identifying...",
            "Perform-livenessdetection": "Processing...",

            "NoMotionDetected": "We could not detect any motion.<br/>For Liveness Detection please nod your head slightly.",
            "NoFaceFound": "We could not find a suitable face.<br/>Come close and look straight before you start.",
            "MultipleFacesFound": "We found multiple faces or a strongly uneven background distracted us.<br/>Your face should fill the circle completely.",
            "LiveDetectionFailed": "Liveness Detection failed.<br/>Look straight into the camera, then nod your head slightly.<br/>Please ensure constant lighting.",
            "ChallengeResponseFailed": "Challenge-Response failed!<br/>Slowly follow the head's movement.",
            "NotRecognized": "You have not been recognized!<br/>Please ensure constant lighting. For improving recognition, please enroll again."
        };

        /* ----------------- Set button functionality ------------------------------------------*/


        // jQuery - shortcut for $(document).ready()
        // Document Object Model (DOM) is ready
        $(function () {
            initialize();

            // set navigation for the buttons
            $('#uuicancel').attr('href', returnURL + '?error=user_abort&access_token=' + token + '&state=' + state);
            $('#uuiskip').attr('href', returnURL + '?error=user_skip&access_token=' + token + '&state=' + state);

            // set url for the BioID mobile app
            if (task === 'verification') {
                $('#uuimobileapp').attr('href', 'bioid-verify://?access_token=' + token + '&return_url=' + returnURL + '&state=' + state);
            }
            else if (task === 'enrollment') {
                $('#uuimobileapp').attr('href', 'bioid-enroll://?access_token=' + token + '&return_url=' + returnURL + '&state=' + state);
            }

            $('#uuiinstruction').attr('data-res', 'UserInstruction-CloseUp');

            // hide button after first click
            $('#uuimobileapp').click(function () {
                $('#uuimobileapp').hide();
            });

            // Check if cookie is set
            // let skipCookie = false;
            // var cookie = document.cookie;
            // if (cookie != "") {
            //    skipCookie = cookie.includes('true');
            // }

            if (skipIntro /* || skipCookie */) {
                showIntroduction(false);
            }
            else {
                showIntroduction(true);
            }
        });

        function showIntroduction(show) {
            if (show) {
                $('#uuiintroduction').show();
            }
            else {
                $('#uuiintroduction').hide();
                $('#uuiwepapp').show();
                initCapture();

                let checked = $('#introskip').prop("checked");
                if (checked) {
                    skipIntro = true;
                    // set cookie (1 year) to skip the introduction for the next time
                    // document.cookie = "BioIDSkipIntro=true;max-age=31536000;path=/;secure;SameSite=Strict";
                }
            }
        }

        function initCapture() {
            // init BWS capture jQuery plugin (see bws.capture.js)
            bwsCapture = bws.initcapture(document.getElementById('uuicanvas'), document.getElementById('livevideo'), token, {
                apiurl: apiurl,
                task: task,
                trait: trait,
                threshold: threshold,
                challengeResponse: challengeResponse,
                recordings: recordings
            });
            let success = initHead();
            if (!success) {
                $('#uuierror').html(formatText('webgl-error'));
                $('#uuiskip').show();
            }
            else {
                // and start everything
                onStart();
            }
        }

        // called from Start button and onStart to initiate a new recording
        function startRecording() {
            $('#uuistart').attr('disabled', 'disabled');
            var tags = challengeResponse && challenges.length > currentExecution && challenges[currentExecution].length > 0 ? challenges[currentExecution] : [];
            bwsCapture.startRecording(tags);
        }

        // called from Mirror button to mirror the captured image
        function mirror() {
            bwsCapture.mirror();
        }


        /* ---------------- Localization of strings ----------------------------------------------*/


        // localization of displayed strings
        function localize() {
            // loops through all HTML elements that must be localized.
            let resourceElements = $('[data-res]');
            for (let i = 0; i < resourceElements.length; i++) {
                let element = resourceElements[i];
                let resourceKey = $(element).attr('data-res');
                if (resourceKey) {
                    // Get all the resources that start with the key.
                    for (let key in localizedData) {
                        if (key.indexOf(resourceKey) === 0) {
                            let value = localizedData[key];
                            // Dot notation in resource key - assign the resource value to the elements property
                            if (key.indexOf('.') > -1) {
                                let attrKey = key.substring(key.indexOf('.') + 1);
                                $(element).attr(attrKey, value);
                            }
                            // No dot notation in resource key, assign the resource value to the element's innerHTML.
                            else if (key === resourceKey) {
                                $(element).html(value);
                            }
                        }
                    }
                }
            }
        }

        // localization and string formatting (additional arguments replace {0}, {1}, etc. in localizedData[key])
        function formatText(key) {
            var formatted = key;
            if (localizedData[key] !== undefined) {
                formatted = localizedData[key];
            }
            for (let i = 1; i < arguments.length; i++) {
                formatted = formatted.replace('{' + (i - 1) + '}', arguments[i]);
            }
            return formatted;
        }


        /* ----------------- Initialize BWS capture jQuery plugin --------------------------------*/


        // initialize - load content in specific language and initialize bws capture
        function initialize() {
            // change title if task is enrollment
            if (task === 'enrollment') {
                $('#uuititle').attr('data-res', 'titleEnrollment');
            }
            // change title if task is identification
            else if (task === 'identification') {
                $('#uuititle').attr('data-res', 'titleIdentification');
            }
            else if (task === 'livenessdetection') {
                $('#uuititle').attr('data-res', 'titleLiveDetection');
            }

            // try to get language info from the browser.
            let userLangAttribute = navigator.language || navigator.userLanguage || navigator.browserLanguage || 'en';
            let userLang = userLangAttribute.slice(0, 2);
            // let userLocation = userLangAttribute.slice(-2) || 'us';

            $.getJSON('./language/' + userLang + '.json').
                done(function (data) {
                    console.log('Loaded the language-specific resource successfully');
                    localizedData = data;
                }).fail(function (textStatus, error) {
                    console.log('Loading of language-specific resource failed with: ' + textStatus + ', ' + error);
                }).always(function () {
                    localize();
                });
        }

        /* ------------------ Start BWS capture jQuery plugin -----------------------------------*/

        // startup code
        function onStart() {
            bwsCapture.start(function () {
                captureStarted();
                $('#uuicanvas').show();
            }, function (error) {

                // hide uuiwebapp
                $('#uuiwebapp').hide();

                // show default information about general issues
                $('#uuisplash').show();

                // show button for continue without biometrics (skip biometric task)
                $('#uuiskip').show();
                // show button for BioID app (interapp communication)
                if (task === 'verification' || task === 'enrollment') {
                    $('#uuimobileapp').show();
                }
                if (error !== undefined) {
                    // different browsers use different errors
                    if (error.code === 1 || error.name === 'PermissionDeniedError') {
                        // in the spec we find code == 1 and name == PermissionDeniedError for the permission denied error
                        $('#uuierror').html(formatText('capture-error', formatText('PermissionDenied')));
                    } else {
                        // otherwise try to print the error
                        $('#uuierror').html(formatText('capture-error', error));
                    }
                } else {
                    // no error info typically says that browser doesn't support getUserMedia
                    $('#uuierror').html(formatText('nogetUserMedia'));
                }
            }, function (error, retry) {
                // done
                stopRecording();
                currentExecution++;

                if (error !== undefined && retry && currentExecution < executions) {
                    console.log('Current Execution: ' + currentExecution);
                } else {
                    // done: redirect to caller ...
                    let url = returnURL + '?access_token=' + token;
                    if (error !== undefined) {
                        url = url + '&error=' + error;
                    }
                    url = url + '&state=' + state + '&skipintro=' + skipIntro;
                    window.location.replace(url);
                }
            }, function (status, message, dataURL) {
                let $msg;
                if (status === 'UploadProgress') {
                    // for single upload status
                    let id = message.id;
                    let modId = ((id - 1) % 4) + 1;
                    // for compact upload status
                    let progresscompact = 0;
                    progressMap.set(id, message.progress);
                    progressMap.forEach(function (value) {return progresscompact += value});
                    progresscompact = Math.ceil(progresscompact / recordings);

                    if (progresscompact > 100) {
                        progresscompact = 100;
                    }

                    // css media query decision
                    if ($('#uuisingleupload').is(':visible') == true) {
                        $('#uuiprogress' + modId).show();
                        $('#uuiprogressbar' + modId).width(message.progress + '%');
                        // if the window size changed
                        $('#uuiprogresscompact').hide();
                    }
                    else {
                        $('#uuiprogresscompact').show();
                        $('#uuiprogressbarcompact').width(progresscompact + '%');
                    }
                }
                else if (status === 'DisplayTag') {
                    setCurrentTag(message);
                    $msg = $('#uuiinstruction');
                    if (challengeResponse || task === 'enrollment') {
                        $msg.html(formatText('UserInstruction-FollowMe'));
                    }
                    else {
                        $msg.html(formatText('UserInstruction-NodYourHead'));
                    }
                    $msg.stop(true).fadeIn();
                } else {
                    // report a message on the screen
                    let msg = formatText(status);

                    // user instructions
                    if (status.indexOf('UserInstruction') > -1) {
                        $msg = $('#uuiinstruction');
                        if (status === 'UserInstruction-Start') {
                            let counter = recordings;
                            if (counter > 4) {
                                counter = 4;
                            }
                            for (let i = 1; i <= counter; i++) {
                                $('#uuiuploaded' + i).hide();
                                $('#uuiupload' + i).hide();
                                $('#uuiwait' + i).show();
                                $('#uuiimage' + i).show();
                                $('#uuiprogress' + i).hide();
                                $('#uuiprogressbar' + i).width(0);
                            }
                            progressMap.clear();
                            $('#uuiprogresscompact').hide();
                            $('#uuiprogressbarcompact').width(0);
                            resetHeadDisplay();
                        }
                        else {
                            $msg.html(msg);
                            $msg.stop(true).fadeIn();
                        }
                    }

                    // perform tasks
                    if (status.indexOf('Perform') > -1 || status.indexOf('Retry') > -1) {
                        // hide compact upload progress
                        $('#uuiprogresscompact').hide();
                    }

                    // results of uploading or perform task
                    if (status.indexOf('Failed') > -1 ||
                        status.indexOf('NotRecognized') > -1 ||
                        status.indexOf('NoFaceFound') > -1 ||
                        status.indexOf('MultipleFacesFound') > - 1) {

                        changeLiveView(true);

                        // show message
                        $('#uuiinstruction').text('');
                        $('#uuistatus').show();
                        $msg = $('#uuimessage');
                        $msg.html(formatText(msg));
                        $msg.stop(true).fadeIn();
                    }

                    // display some animations/images depending on the status
                    let uploaded = bwsCapture.getUploaded();
                    let recording = uploaded + bwsCapture.getUploading();
                    // use modulo calculation for images more than 4
                    let modRecording = ((recording - 1) % 4) + 1;
                    let modUploaded = ((uploaded - 1) % 4) + 1;

                    if (status === 'Uploading') {
                        // begin an upload - current image
                        $('#uuiwait' + modRecording).hide();
                        $('#uuiupload' + modRecording).show();
                        $('#uuiuploaded' + modRecording).hide();

                        // if uuiuploaded is not visible -> mobile view
                        if (recording >= recordings) {
                            $('#uuiinstruction').html(formatText('UserInstruction-PleaseWait'));
                            changeLiveView(true);
                        }
                    } else if (status === 'Uploaded') {
                        // successfull upload (we should have a dataURL)
                        if (dataURL) {
                            $('#uuiupload' + modUploaded).hide();
                            $('#uuiprogress' + modUploaded).hide();
                            let $image = $('#uuiuploaded' + modUploaded);
                            $image.attr('src', dataURL);
                            $image.show();
                        }
                    } else if (status === 'NoFaceFound' || status === 'MultipleFacesFound') {
                        // upload failed
                        recording++;
                        modRecording = ((recording - 1) % 4) + 1;
                        $('#uuiupload' + modRecording).hide();
                        $('#uuiwait' + modRecording).show();
                    }
                }
            });
        }

        // switch between liveview and displayed messages
        function changeLiveView(blur) {
            if (blur) {
                // hide head and blur canvas
                hideHead();
                $('#uuicanvas').css('filter', 'blur(10px)');
                $('#uuiprogresscompact').hide();
            }
            else {
                $('#uuicanvas').css('filter', 'none');
                showHead();
            }
        }

        // called by onStart to update GUI
        function captureStarted() {
            $('#uuiwebapp').show();
            $('#uuimessage').show();
            $('#uuiinstruction').show();

            // Currently not neccessary - therefore the button is not shown!
            // $('#uuimirror').show().click(mirror);

            $('#uuistart').show().click(function () {startRecording(task === 'enrollment');});


            $('#uuiok').show().click(function () {
                $('#uuistatus').hide();
                $('#uuistart').prop('disabled', false);
                $('#uuiinstruction').html(formatText('UserInstruction-CloseUp'));
                changeLiveView(false);
            });

            setTimeout(function () {console.log('triggered showHead'); showHead();}, 50);

        }

        // called from onStart when recording is done
        function stopRecording() {
            hideHead();

            bwsCapture.stopRecording();

            for (let i = 1; i <= 4; i++) {
                $('#uuiimage' + i).hide();
            }
        }

        /* -------------------- Displaying head ---------------------------------------------------*/

        var camera, scene, renderer, id;
        var startTime;
        var resetHead = false;
        const maxVertical = 0.20;
        const maxHorizontal = 0.25;

        function initHead() {
            // renderer
            try {
                renderer = new THREE.WebGLRenderer({alpha: true});
            }
            catch (e) {
                return false;
            }

            let container = document.getElementById('uuihead');
            document.body.appendChild(container);

            let width = $('#uuihead').width();
            let height = $('#uuihead').height();
            let uuihead = $('#uuihead');
            $('#uuiliveview').append(uuihead);

            // camera
            camera = new THREE.PerspectiveCamera(20, width / height, 1, 1000);
            camera.position.set(0, 0, 5.5);

            // scene
            scene = new THREE.Scene();
            let ambientLight = new THREE.AmbientLight(0x4953FF, 0.4);
            scene.add(ambientLight);
            let pointLight = new THREE.PointLight(0x3067FF, 0.8);
            camera.add(pointLight);
            scene.add(camera);

            // texture
            let manager = new THREE.LoadingManager();
            manager.onProgress = function (item, loaded, total) {
                console.log(item, loaded, total);
            };

            // model
            let onProgress = function (xhr) {
                if (xhr.lengthComputable) {
                    let percentComplete = xhr.loaded / xhr.total * 100;
                    console.log(Math.round(percentComplete, 2) + '% downloaded');
                }
            };
            let onError = function (xhr) { };
            let loader = new THREE.OBJLoader(manager);
            let material = new THREE.MeshLambertMaterial({transparent: false, opacity: 0.8});

            loader.load('./model/head.obj', function (head) {
                head.traverse(function (child) {
                    if (child instanceof THREE.Mesh) {
                        //   child.material = material;
                    }
                });
                head.name = 'BioIDHead';
                head.position.y = 0;
                scene.add(head);
            }, onProgress, onError);

            renderer.setClearColor(0x000000, 0); // the default
            renderer.setPixelRatio(window.devicePixelRatio);
            renderer.setSize(width, height);

            container.appendChild(renderer.domElement);
            document.addEventListener('uuiresize', onHeadResize, false);

            return true;
        }

        function onHeadResize() {

            let canvasWidth = parseInt($('#uuicanvas').width());
            let canvasHeight = parseInt($('#uuicanvas').height());

            $('#uuihead').css({'margin-top': -canvasHeight, 'margin-left': '0'});
            $('#uuihead').attr('width', canvasWidth);
            $('#uuihead').attr('height', canvasHeight + 50);

            camera.aspect = canvasWidth / canvasHeight;
            camera.updateProjectionMatrix();
            renderer.setSize(canvasWidth, canvasHeight);
            renderer.render(scene, camera);
        }

        function resetHeadDisplay() {
            currentTag = '';
            parentTag = '';
            resetHead = true;
            cancelAnimationFrame(id);
            $('.head').css('opacity', '0.6');
        }

        function setCurrentTag(tag) {
            if (currentTag !== '') {
                parentTag = currentTag;
            }

            currentTag = tag;
            startTime = new Date().getTime();

            if (currentTag === 'any' && task !== 'enrollment') {
                constantAnimation();
            }
            else {
                animateHead();
                console.log('DisplayTag: ' + tag);
            }
        }

        function constantAnimation() {
            // change css class 'head'
            $('.head').css('opacity', '0.8');

            // animation time
            let delta = 0.005;
            let head = scene.getObjectByName('BioIDHead');
            showHead();

            let direction = 'down';
            var animate = function () {

                if (direction === 'up') {
                    if (head.rotation.x >= -maxVertical) {
                        head.rotation.x -= delta;
                    }
                    else {
                        direction = 'down';
                    }
                }

                if (direction === 'down') {
                    if (head.rotation.x <= maxVertical) {
                        head.rotation.x += delta;
                    }
                    else {
                        direction = 'up';
                    }
                }

                id = requestAnimationFrame(animate);
                renderer.render(scene, camera);
            };
            animate();
        }

        function animateHead() {
            // animation time
            let speed = 0.000005;
            let endTime = new Date().getTime();
            let deltaTime = (endTime - startTime);
            let delta = deltaTime * speed;

            let head = scene.getObjectByName('BioIDHead');
            let doAnimation = false;

            if (head) {
                if (resetHead) {
                    // reset head rotation to center
                    head.rotation.x = 0;
                    head.rotation.y = 0;
                    resetHead = false;
                    doAnimation = true;
                    // change css class 'head'
                    $('.head').css('opacity', '0.8');
                    showHead();
                }
                else {
                    if (currentTag === 'any') {
                        if (task === 'enrollment') {
                            // get predefined direction for better enrollment
                            let recording = bwsCapture.getUploaded() + bwsCapture.getUploading() - 1;
                            currentTag = enrollmentTags[recording];
                        }
                        else {
                            if (head.rotation.x >= -maxVertical && head.rotation.x <= 0) {
                                head.rotation.x -= delta;
                                doAnimation = true;
                            }
                            else {
                                head.rotation.x += delta;
                                doAnimation = true;
                            }
                        }
                    }

                    if (currentTag === 'down') {
                        head.rotation.y = 0;
                        if (parentTag === 'up') {
                            if (head.rotation.x <= 0) {
                                head.rotation.x += delta;
                                doAnimation = true;
                            }
                        }
                        else {
                            if (head.rotation.x >= 0 && head.rotation.x < maxVertical) {
                                head.rotation.x += delta;
                                doAnimation = true;
                            }
                        }
                    }
                    else if (currentTag === 'up') {
                        head.rotation.y = 0;
                        if (parentTag === 'down') {
                            if (head.rotation.x >= 0) {
                                head.rotation.x -= delta;
                                doAnimation = true;
                            }
                        }
                        else {
                            if (head.rotation.x >= -maxVertical && head.rotation.x <= 0) {
                                head.rotation.x -= delta;
                                doAnimation = true;
                            }
                        }
                    }
                    else if (currentTag === 'left') {
                        head.rotation.x = 0;
                        if (parentTag === 'right') {
                            if (head.rotation.y >= 0) {
                                head.rotation.y -= delta;
                                doAnimation = true;
                            }
                        }
                        else {
                            if (head.rotation.y >= -maxHorizontal && head.rotation.y <= 0) {
                                head.rotation.y -= delta;
                                doAnimation = true;
                            }
                        }
                    }
                    else if (currentTag === 'right') {
                        head.rotation.x = 0;
                        if (parentTag === 'left') {
                            if (head.rotation.y <= 0) {
                                head.rotation.y += delta;
                                doAnimation = true;
                            }
                        }
                        else {
                            if (head.rotation.y >= 0 && head.rotation.y <= maxHorizontal) {
                                head.rotation.y += delta;
                                doAnimation = true;
                            }
                        }
                    }
                }

                if (doAnimation) {
                    id = requestAnimationFrame(animateHead);
                }
                renderer.render(scene, camera);
            }
        }

        function showHead() {

            $('#uuihead').show();
            onHeadResize();
            console.log('showHead')
        }

        function hideHead() {
            $('#uuihead').hide();
            resetHeadDisplay();
            console.log('hideHead');
        }

    </script>
</body>

</html>
