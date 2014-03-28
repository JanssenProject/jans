<?php
namespace org\xdi\oxauth\model\common;
require_once 'HasParamName.php';

/**
 * An ASCII string value that specifies how the Authorization Server displays
 * the authentication and consent user interface pages to the End-User.
 *
 * @author Gabin Dongmo Date: 29/03/2013
 */
class Display implements HasParamName {
    
    /**
    * The Authorization Server SHOULD display authentication and consent UI
    * consistent with a full user-agent page view. If the display parameter
    * is not specified this is the default display mode.
     */
    const PAGE = "page";
    /**
    * The Authorization Server SHOULD display authentication and consent UI
    * consistent with a popup user-agent window. The popup user-agent window
    * SHOULD be 450 pixels wide and 500 pixels tall.
     */
    const POPUP = "popup";
    /**
    * The Authorization Server SHOULD display authentication and consent UI
    * consistent with a device that leverages a touch interface.
    * The Authorization Server MAY attempt to detect the touch device and
    * further customize the interface.
     */
    const TOUCH = "touch";
    /**
    * The Authorization Server SHOULD display authentication and consent UI
    * consistent with a "feature phone" type display.
     */
    const WAP = "wap";
    /**
     * The Authorization Server SHOULD display authentication and consent UI
     * consistent with the limitations of an embedded user-agent.
     */
    const EMBEDDED = "embedded";
    
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
     * Returns an array containing the constants of this enum type, in the 
     * order they are declared. This method may be used to iterate over the 
     * constants as follows:
     * 
     * <pre>
     * foreach (Display::values() as $value) {
     *      return $value;
     * }
     * </pre>
     * 
     * @return Array An array containing the constants of this enum type, in the 
     * order they are declared
     */
    public static function values(){
            return new \ArrayIterator(
                        array(
                            Display::PAGE,
                            Display::POPUP,
                            Display::TOUCH,
                            Display::WAP,
                            Display::EMBEDDED
                        )
                    );
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
}

?>
