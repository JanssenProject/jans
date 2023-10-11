//
//  UIApplication+Alert.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 11.10.2023.
//

import UIKit

extension UIApplication {

    class func getTopViewController(base: UIViewController? = UIApplication.shared.windows.first { $0.isKeyWindow }?.rootViewController) -> UIViewController? {

        if let nav = base as? UINavigationController {
            return getTopViewController(base: nav.visibleViewController)

        } else if let tab = base as? UITabBarController, let selected = tab.selectedViewController {
            return getTopViewController(base: selected)

        } else if let presented = base?.presentedViewController {
            return getTopViewController(base: presented)
        }
        return base
    }
    
    class func showAlert(message: String) {
        let alert = UIAlertController(title: "Error", message: message, preferredStyle: .alert)
        // Create OK button with action handler
        let ok = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            print("Alert dissmissed")
        })
        alert.addAction(ok)
        
        UIApplication.getTopViewController()?.present(alert, animated: true)
    }
}
