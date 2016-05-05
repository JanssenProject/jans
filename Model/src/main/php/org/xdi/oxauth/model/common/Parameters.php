<?php
namespace org\xdi\oxauth\model\common;

/**
 * Description of Parameters
 *
 * @author Gabin Dongmo Date: 29/03/2013
 */
class Parameters implements HasParamName {
    
    /**
     * Session State
     */
    const SESSION_STATE = "session_state";
    
    /**
     * Request Session State
     */
    const REQUEST_SESSION_STATE = "request_session_state";
    
    /**
     * Parameter Name
     * @var String 
     */
    private $paramName;
    
    /**
     * Name to append
     * @var String 
     */
    private $nameToAppend;
    
    /**
     * Constructor
     * @param String Parameter
     */
    public function __construct($parameter = "") {
        $this->paramName = $parameter;
        $this->nameToAppend = "&" + $this->paramName + "=";
    }
    
    /**
     * Gets the parameter name entered
     * @return String Parameter name
     */
    public function getParamName() {
        return $this->paramName;
    }
    
    /**
     * Get the paramater appended
     * @return String Name to append
     */
    public function nameToAppend(){
        return $this->nameToAppend;
    }
}



?>
