//
//  ContentView.swift
//  CedaringIos
//
//  Created by Arnab Dutta on 21/01/25.
//

import SwiftUI

struct ContentView: View {
    @State private var showModal = false
    @State private var bootstrapConfig: [String: Any] = Helper.dictionaryFromFile(filename: "bootstrap");
    @State private var tokens: [String: String] =
    Helper.dictionaryFromFile(filename: "tokens");
    @State private var action: String = "Jans::Action::\"Update\"";
    @State private var resource: [String: Any] =
    Helper.dictionaryFromFile(filename: "resource");
    @State private var context: [String: Any] = Helper.dictionaryFromFile(filename: "context");
    @State private var resultMessage = "";
    @State private var combinedLogs: String = "";
    
    @State private var modelTitle: String = ""
    @State private var modelSubTitle: String = ""
    @State private var modelFieldName: String = ""
    @State private var modelFieldValue: String = ""
    
    var body: some View {
        VStack {
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundStyle(.tint)
            ZStack {
                RoundedRectangle(cornerRadius: 25)
                    .fill(.white)
                Button("Bootstrap Config") {
                    showModal = true;
                    modelTitle = "Bootstrap Config";
                    modelSubTitle = "";
                    modelFieldName = "";
                    modelFieldValue = Helper.dictionaryToString( bootstrapConfig);
                }
            }
            ForEach(tokens.keys.sorted(), id: \.self) { tokenName in
                if let tokenValue = tokens[tokenName] {
                    ZStack {
                        RoundedRectangle(cornerRadius: 25)
                            .fill(.white)
                        Button(tokenName) {
                            showModal = true;
                            modelTitle = tokenName;
                            modelSubTitle = "";
                            modelFieldName = "";
                            modelFieldValue = tokenValue as! String;
                        }
                    }
                    //.frame(width: 450, height: 50)
                }
            }
            
            ZStack {
                RoundedRectangle(cornerRadius: 25)
                    .fill(.white)
                Button("Action") {
                    showModal = true;
                    modelTitle = "Action";
                    modelSubTitle = "";
                    modelFieldName = "";
                    modelFieldValue = action;
                }
            }
            
            ZStack {
                RoundedRectangle(cornerRadius: 25)
                    .fill(.white)
                Button("Resource") {
                    showModal = true;
                    modelTitle = "Resource";
                    modelSubTitle = "";
                    modelFieldName = "";
                    modelFieldValue = Helper.dictionaryToString( resource);
                }
            }
            
            ZStack {
                RoundedRectangle(cornerRadius: 25)
                    .fill(.white)
                Button("Context") {
                    showModal = true;
                    modelTitle = "Context";
                    modelSubTitle = "";
                    modelFieldName = "";
                    modelFieldValue = Helper.dictionaryToString( context)
                }
            }
            
            Button(action: {
                print("Form submitted..")
                performAuthorization(bootstrapConfig:bootstrapConfig,
                                     tokens: tokens,
                                     action: action,
                                     resource: resource,
                                     context: context
                )
                showModal = true;
                modelTitle = "This is a Authz Result!";
                modelSubTitle = resultMessage;
                modelFieldName = "Logs";
                modelFieldValue = combinedLogs;
                
            }) {
                Text("Submit")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.green)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            .padding(.horizontal)
            .sheet(isPresented: $showModal) {
                
                
                ModalView(title: $modelTitle,
                          resultMessage: $modelSubTitle,
                          fieldName: $modelFieldName,
                          fieldValue: $modelFieldValue)
            }
            Text(resultMessage).padding()
        }
        .padding()
        .background(Color(.systemGray6)) // Light grey background
        .edgesIgnoringSafeArea(.all)
        
    }
    
    func validateBootstrap(bootstrapConfig: String) -> Bool{
        return true;
    }
    
    
    func performAuthorization(
        bootstrapConfig: [String: Any],
        tokens: [String: String],
        action: String,
        resource: [String: Any],
        context: [String: Any]
    ) -> String {
        resultMessage = "Authorization not performed."
        
        do {
            if let policyStoreUrl = Bundle.main.path(forResource: "policy-store", ofType: "json") {
                self.bootstrapConfig["CEDARLING_POLICY_STORE_LOCAL_FN"] = policyStoreUrl;
            }
            let bootstrapConfigStr = Helper.dictionaryToString(self.bootstrapConfig);
            let instance = try Cedarling.loadFromJson(config: bootstrapConfigStr)
            
            if let payloadJsonData = try? JSONSerialization.data(withJSONObject: resource["payload"], options: []),
               let payloadJsonString = String(data: payloadJsonData, encoding: .utf8) {
                print(payloadJsonString)
                
                
                let result: AuthorizeResult = try instance.authorize(
                    tokens: tokens,
                    action: action,
                    resourceType: resource["resource_type"] as! String,
                    resourceId: resource["resource_id"] as! String,
                    payload: payloadJsonString,
                    context: Helper.dictionaryToString( context)
                )
                
                let logs = try instance.popLogs();
                combinedLogs = logs.joined(separator: ", ").replacingOccurrences(of: "\\\"", with: "\"")
                
                print("Logs:\(combinedLogs)")
                
                resultMessage = "Authorization successful: \(result.decision) , \(result.jsonWorkload) , \(result.jsonPerson)"
            } else {
                print("This is a error message: unable to decode payload")
                
                resultMessage = "Unexpected error occurred: unable to decode payload"
            }
        } catch {
            print("This is a error message\(error)")
            
            resultMessage = "Unexpected error occurred: \(error)"
        }
        return resultMessage
    }
    
}

struct ModalView: View {
    @Environment(\.presentationMode) var presentationMode
    @Binding var title: String
    @Binding var resultMessage: String
    @Binding var fieldName: String
    @Binding var fieldValue: String
    var body: some View {
        VStack {
            Text(title) //"This is a Authz Result!"
                .font(.title)
                .padding()
            Text("\(resultMessage)")
                .padding()
            CustomTextField(title:
                                fieldName, // Logs
                            text: $fieldValue,
                            isMultiline: true
            )
            Button("Close") {
                presentationMode.wrappedValue.dismiss()
            }
            .padding()
        }
    }
}

struct CustomTextField: View {
    let title: String
    @Binding var text: String
    var isMultiline: Bool = false
    
    var body: some View {
        VStack(alignment: .leading) {
            Text(title)
                .font(.caption)
                .foregroundColor(.gray)
                .padding(.horizontal)
            
            if isMultiline {
                ScrollView {
                    TextEditor(text: $text)
                        .frame(minHeight: 400) // Set a height
                        .padding()
                        .background(Color.white)
                        .cornerRadius(8)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.gray, lineWidth: 1))
                        .padding(.horizontal)
                }
            } else {
                TextField(title, text: $text)
                    .padding()
                    .background(Color.white)
                    .cornerRadius(8)
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.gray, lineWidth: 1))
                    .padding(.horizontal)
            }
        }
    }
}
#Preview {
    ContentView()
}
