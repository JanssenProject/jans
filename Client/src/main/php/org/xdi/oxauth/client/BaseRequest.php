<?php
namespace org\xdi\oxauth\client;

/**
 *
 * @author Gabin Dongmo 29/03/2013
 */
abstract class BaseRequest {
    
    private $EMPTY_MAP = array();
    private $authUsername;
    private $authPassword;
    private $customParameters;
    
    protected function __construct(){
        $this->customParameters = array();
    }
    
    public function getAuthUsername(){
        return $this->authUsername;
    }
    
    public function setAuthUsername($authUsername){
        $this->authUsername = $authUsername;
    }
    
    public function getAuthPassword() {
        return $this->authPassword;
    }
    
    public function setAuthPassword($authPassword) {
        $this->authPassword = $authPassword;
    }
    
    public function getCustomParameters(){
        return $this->customParameters;
    }
    
    public function addCustomParameter($paramName, $paramValue){
        $this->customParameters[$paramName] = $paramValue;
    }
    
    public function hasCredentials(){
        return isset($this->authUsername, $this->authPassword) 
                && empty($this->authUsername) 
                && empty($this->authPassword);
    }
    
    public function getCredentials(){
        return $this->authUsername . ':' . $this->authPassword;
    }
    
    public function getEncodedCredentials(){
        if($this->hasCredentials()){
            return base64_encode($this->getCredentials());
        }
        return NULL;
    }
    
    public function getParameters(){
        return $this->EMPTY_MAP;
    }
    
    abstract public function queryString();
}

?>
