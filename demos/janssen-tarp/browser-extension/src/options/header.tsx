import React from "react";
import { HelpCircle } from 'lucide-react';
import HelpDrawer from "./helpDrawer";

const Header = () => {
  const [isHelpOpen, setIsHelpOpen] = React.useState(false);

  const handleDrawer = (isOpen: boolean) => {
    setIsHelpOpen(isOpen);
  };
  return (
    <header className="bg-white border-b border-gray-200 px-8 py-4">
      <HelpDrawer isOpen={isHelpOpen} handleDrawer={handleDrawer} />
      <div className="flex items-center justify-between max-w-[1400px] mx-auto">
        <div className="flex items-center gap-2 px-7">
          <div className="flex items-center">
            <img
              src="logo.jpg"
              alt="Jans TARP"
              className="h-12 w-auto object-contain"
            />
          </div>
        </div>
        <div className="flex items-center gap-2 px-7">
          <button onClick={() => handleDrawer(true)} className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors">
            <HelpCircle className="w-5 h-5 text-gray-600" />
            <span className="text-gray-700 font-medium text-base">Need help?</span>
          </button>
        </div>
      </div>
    </header>
  );
};

export default Header;
