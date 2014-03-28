<?php
namespace org\xdi\oxauth\model\common;

/**
 * <p>
 * This class allows to enumerate and identify the possible values of the
 * parameter response_type for the authorization endpoint.
 * </p>
 * <p>
 * The client informs the authorization server of the desired grant type.
 * </p>
 * <p>
 * The authorization endpoint is used by the authorization code grant type and
 * implicit grant type flows.
 * </p>
 *
 * @author Gabin Dongmo Date: 29/03/2013
 */
class ResponseType implements HasParamName {
    
    /**
     * Used for the authorization code grant type.
     */
    const CODE = "code";
    /**
     * Used for the implicit grant type.
     */
    const TOKEN = "token";
    /**
     * Include an ID Token in the authorization response.
     */
    const ID_TOKEN = "id_token";
    
    /**
     *  Parameter
     * @var String 
     */
    private $paramName;
    
    /**
     * The constructor sets the parameter
     * @param String Parameter
     */
    public function __construct($parameter = "") {
        $this->paramName = $parameter;
    }
    
    /**
     * Gets the parameter name entered
     * @return String Parameter
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
     * foreach (ResponseType::values() as $value) {
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
                            ResponseType::CODE,
                            ResponseType::ID_TOKEN,
                            ResponseType::TOKEN
                        )
                    );
    }
    
}

?>
