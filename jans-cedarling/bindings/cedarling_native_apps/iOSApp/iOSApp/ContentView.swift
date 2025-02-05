//
//  ContentView.swift
//  CedaringIos
//
//  Created by Arnab Dutta on 21/01/25.
//

import SwiftUI

struct ContentView: View {
    @State private var showModal = false
    @State private var bootstrapConfig: Bootstrap = Bootstrap();
    @State private var tokens: Tokens = Tokens();
    @State private var action: String = "Jans::Action::\"Update\"";
    @State private var resource: Resource = Resource();
    @State private var context: String = "{}";
    @State private var resultMessage = "";
    @State private var combinedLogs: String = "";
    
    var body: some View {
        VStack {
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundStyle(.tint)
            
            CustomTextField(title:
                                "Bootstrap Config",
                            text: $bootstrapConfig.json,
                            isMultiline: true
            )
            .onSubmit {
                validateBootstrap(bootstrapConfig: bootstrapConfig.json)
            }
            
            CustomTextField(title:
                                "Access Token",
                            text: $tokens.accessToken,
                            isMultiline: false
            )
            CustomTextField(title:
                                "Id Token",
                            text: $tokens.idToken,
                            isMultiline: false
            )
            CustomTextField(title: "UserInfo Token",
                            text: $tokens.userInfoToken,
                            isMultiline: false
            )
            CustomTextField(title:
                                "Action",
                            text: $action,
                            isMultiline: false
            )
            
            
            CustomTextField(title:
                                "Resource Type",
                            text: $resource.resource_type,
                            isMultiline: false
            )
            
            CustomTextField(title:
                                "Resource Id",
                            text: $resource.resource_id,
                            isMultiline: false
            )
            

            CustomTextField(title:
                                "Context",
                            text: $context,
                            isMultiline: false
            )
            
            
            Button(action: {
                print("Form submitted..")
                performAuthorization(bootstrapConfig: bootstrapConfig.json,
                                     accessToken: tokens.accessToken,
                                     idToken: tokens.idToken,
                                     userInfoToken: tokens.userInfoToken,
                                     action: action,
                                     resource: resource,
                                     context: context
                )
                showModal = true;
                
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
                ModalView(resultMessage: $resultMessage, combinedLogs: $combinedLogs)
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
        bootstrapConfig: String,
        accessToken: String,
        idToken: String,
        userInfoToken: String,
        action: String,
        resource: Resource,
        context: String
    ) -> String {
        print("This is a string message")
        
        resultMessage = "Authorization not performed."
        
        do {
            let instance = try Cedarling.loadFromJson(config: bootstrapConfig)
            
            if let payloadJsonData = try? JSONSerialization.data(withJSONObject: resource.payload, options: []),
               let payloadJsonString = String(data: payloadJsonData, encoding: .utf8) {
                print(payloadJsonString)
                
                
                let result: AuthorizeResult = try instance.authorize(
                    accessToken: accessToken,
                    idToken: idToken,
                    userinfoToken: userInfoToken,
                    action: action,
                    resourceType: resource.resource_type,
                    resourceId: resource.resource_id,
                    payload: payloadJsonString,
                    context: context
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
    @Binding var resultMessage: String
    @Binding var combinedLogs: String
    var body: some View {
        VStack {
            Text("This is a Authz Result!")
                .font(.title)
                .padding()
            Text("\(resultMessage)")
                .padding()
            CustomTextField(title:
                                "Logs",
                            text: $combinedLogs,
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
                TextEditor(text: $text)
                    .frame(minHeight: 80, maxHeight: 80) // Set a height
                    .padding()
                    .background(Color.white)
                    .cornerRadius(8)
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.gray, lineWidth: 1))
                    .padding(.horizontal)
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
