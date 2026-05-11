import * as React from 'react';
import CedarlingMgmt from '../features/cedarling/components/CedarlingMgmt';
import Utils from './Utils';
import { OIDCClients, UserDetails } from '../features/authentication';
import { AIAgent } from '../ai/agentUI/index'

interface HomePageProps {
  data: any; // or define the actual shape of data expected by HomePage
  notifyOnDataChange: () => void;
}

export default function HomePage({ data, notifyOnDataChange }: HomePageProps) {

  const [activeTab, setActiveTab] = React.useState('authentication');
  const isLoggedIn = !Utils.isEmpty(data?.loginDetails) && Object.keys(data?.loginDetails ?? {}).length !== 0;

  return (
    <main className="max-w-[1400px] mx-auto px-8 py-8">
      {/* Tabs */}
      <div className="bg-white rounded-lg shadow-sm mb-6">
        <div className="flex border-b border-gray-200">
          <button
            onClick={() => setActiveTab('authentication')}
            className={`px-6 py-4 font-medium text-base transition-colors relative ${activeTab === 'authentication'
              ? 'text-emerald-600'
              : 'text-gray-600 hover:text-gray-800'
              }`}
          >
            {isLoggedIn ? 'User Details' : 'Authentication'}
            {activeTab === 'authentication' && (
              <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-emerald-600" />
            )}
          </button>
          <button
            onClick={() => setActiveTab('cedarling')}
            className={`px-6 py-4 font-medium text-base transition-colors relative ${activeTab === 'cedarling'
              ? 'text-emerald-600'
              : 'text-gray-600 hover:text-gray-800'
              }`}
          >
            Cedarling
            {activeTab === 'cedarling' && (
              <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-emerald-600" />
            )}
          </button>
          <button
            onClick={() => setActiveTab('ai-agent')}
            className={`px-6 py-4 font-medium text-base transition-colors relative ${activeTab === 'ai-agent'
              ? 'text-emerald-600'
              : 'text-gray-600 hover:text-gray-800'
              }`}
          >
            AI Agent
            {activeTab === 'ai-agent' && (
              <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-emerald-600" />
            )}
          </button>
        </div>
      </div>
      {activeTab === 'authentication' && isLoggedIn && 
        <UserDetails
          data={data.loginDetails}
          notifyOnDataChange={notifyOnDataChange}
        />
      }
      {!isLoggedIn && activeTab === 'authentication' &&
        <OIDCClients
          data={data.oidcClients}
          notifyOnDataChange={notifyOnDataChange}
        />
      }
      {activeTab === 'cedarling' && (
        <CedarlingMgmt
          data={data}
          isLoggedIn={isLoggedIn}
          notifyOnDataChange={notifyOnDataChange}
        />
      )}
      {activeTab === 'ai-agent' && (
        <AIAgent
          notifyOnDataChange={notifyOnDataChange}
        />
      )}
    </main>
  );
}
