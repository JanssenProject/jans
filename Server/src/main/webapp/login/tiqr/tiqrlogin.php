<?php

include_once("include.php");
require_once("java.inc"); 

$as = new SimpleSAML_Auth_Simple('authTiqr');
$as->requireAuth();

if ($as->isAuthenticated()) {
	$session = java_session();

	$attributes = $as->getAttributes();

	$uid = "";
	if (isset($attributes["urn:mace:dir:attribute-def:uid"])) {
	    $uid = $attributes["urn:mace:dir:attribute-def:uid"][0];
	} else {
	    $uid = $attributes["uid"][0];
	}

	$session->put('tiqr_user_uid', $uid); 
	$requestURI = http_build_query($_GET);

#	$as->logout();

        $urlHost = SimpleSAML_Utilities::selfURLhost();
	SimpleSAML_Utilities::redirect($urlHost . "/oxauth/postlogin.seam?" . $requestURI);
}
