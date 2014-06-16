<?php

require_once('/var/simplesamlphp/lib/_autoload.php');

function renderTemplate($file, $data=array())
{
    ob_start();
    include($file);
    $content = ob_get_contents();
    ob_end_clean();
    return $content;
}
