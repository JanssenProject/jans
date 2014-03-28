<?php
namespace org\xdi\oxauth\model\common;

/**
 * An ASCII string values that specifies whether the Authorization Server
 * prompts the End-User for re-authentication and consent.
 *
 * @author Gabin Dongmo Date: 29/03/2013
 */
class Prompt implements HasParamName {
    
    /**
     * The Authorization Server MUST NOT display any authentication or
     * consent user interface pages. An error is returned if the End-User
     * is not already authenticated or the Client does not have pre-configured
     * consent for the requested scopes. This can be used as a method to
     * check for existing authentication and/or consent.
     */
    const NONE = "none";
    /**
     * The Authorization Server MUST prompt the End-User for re-authentication
     */
    const LOGIN = "login";
    /**
     * The Authorization Server MUST prompt the End-User for consent before
     * returning information to the Client.
     */
    const CONSENT = "consent";
    /**
     * The Authorization Server MUST prompt the End-User to select a user account.
     * This allows a user who has multiple accounts at the Authorization Server to
     * select amongst the multiple accounts that they may have current sessions for.
     */
    const SELECT_ACCOUNT = "select_account";
    
    /**
     *  Parameter name
     * @var String 
     */
    private $paramName;
    
    /**
     * The constructor sets the parameter name
     * @param String Parameter
     */
    public function __construct($parameter = "") {
        $this->paramName = $parameter;
    }
    
    /**
     * Gets the parameter name entered
     * @return String Parameter name
     */
    public function getParamName() {
        return $this->paramName;
    }
    
    /**
     * Returns a string representation of the object. In this case the 
     * parameter name.
     * 
     * @return String Parameter name
     */
    public function toString(){
        return $this->paramName;
    }
    
    /**
     * Returns an array containing the constants of this enum type, in the 
     * order they are declared. This method may be used to iterate over the 
     * constants as follows:
     * 
     * <p>
     * foreach (Prompt::values() as $value) {
     * </p>
     * <p>
     *      return $value;
     * </p>
     * }
     * 
     * @return Array An array containing the constants of this enum type, in the 
     * order they are declared
     */
    public static function values(){
            return new \ArrayIterator(
                        array(
                            Prompt::CONSENT,
                            Prompt::LOGIN,
                            Prompt::NONE,
                            Prompt::SELECT_ACCOUNT
                        )
                    );
    }
    
}

?>
