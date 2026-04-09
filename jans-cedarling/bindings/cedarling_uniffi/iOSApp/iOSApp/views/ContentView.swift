import SwiftUI

struct ContentView: View {
    @State private var showModal = false
    @State private var bootstrapConfig: [String: Any] = Helper.dictionaryFromFile(filename: "bootstrap")
    @State private var tokens: [TokenMapping] = Helper.loadTokens(fromFileName: "tokens")
    @State private var action: String = "Jans::Action::\"Read\""
    @State private var resource: [String: Any] = Helper.dictionaryFromFile(filename: "resource")
    @State private var context: [String: Any] = Helper.dictionaryFromFile(filename: "context")
    @State private var cjarBytes = Helper.zipToBytes(fileName: "MyStore.cjar")
    @State private var resultMessage = ""
    
    @State private var modelTitle: String = ""
    @State private var modelTextField: String = ""
    @State private var modelJsonField: String = ""
    @State private var modelJsonLogField: String = ""
    @State private var isAuthzRequest = false
    @State private var authzRequestType: AuthzRequestType = .none
    
    @State private var jsonData: Any?
    @State private var selectedLogTypeOption: String = "Decision"
    
    let logOptions = ["Decision", "System", "Metric"]
    
    enum AuthzRequestType {
        case none, result, logs
    }
    
    var body: some View {
        VStack {
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundStyle(.tint)
            
            VStack(spacing: 10) {
                DataButton(title: Labels.BOOTSTRAP_CONFIG, data: bootstrapConfig)
                ForEach(tokens.sorted(by: { $0.mapping < $1.mapping })) { token in
                    DataButton(
                        title: token.mapping,
                        data: token.payload
                    )
                }
                DataButton(title: Labels.ACTION, data: action)
                DataButton(title: Labels.RESOURCE, data: resource)
                DataButton(title: Labels.CONTEXT, data: context)
            }
            
            VStack {
                Text("Selected Log Type: \(selectedLogTypeOption)")
                    .font(.headline)
                    .padding()
                
                Picker(Labels.CHOOSE_OPTION, selection: $selectedLogTypeOption) {
                    ForEach(logOptions, id: \.self) { Text($0).tag($0) }
                }
                .pickerStyle(.segmented)
                .padding()
            }
            
            HStack {
                ActionButton(title: "Submit", color: .green) {
                    performAuthorization(authzRequestType: .result)
                }
                ActionButton(title: "Logs", color: .green) {
                    performAuthorization(authzRequestType: .logs)
                }
            }
            .padding(.horizontal)
        }
        .padding()
        .background(Color(.systemGray6))
        .edgesIgnoringSafeArea(.all)
        .sheet(isPresented: $showModal) {
            ModalView(
                title: $modelTitle,
                modelTextField: $modelTextField,
                modelJsonField: $modelJsonField,
                modelJsonLogField: $modelJsonLogField,
                isAuthzRequest: $isAuthzRequest,
                authzRequestType: $authzRequestType
            )
        }
    }
    
    func performAuthorization(authzRequestType: AuthzRequestType) {
        resultMessage = Labels.AUTHZ_NOT_PERFORMED
        
        do {
            
            let bootstrapConfigStr = Helper.dictionaryToString(bootstrapConfig)
            
            guard let cjarBytes, !cjarBytes.isEmpty else {
                let message = "Policy archive MyStore.cjar is missing or unreadable."
                resultMessage = message
                modelTitle = "Authorization Error"
                modelTextField = message
                modelJsonField = ""
                modelJsonLogField = ""
                isAuthzRequest = false
                self.authzRequestType = .none
                showModal = true
                return
            }
            let instance = try Cedarling.loadFromJsonWithArchiveBytes(
                config: bootstrapConfigStr,
                archiveBytes: Data(cjarBytes)
            )
            
            let resourceStr = Helper.dictionaryToString(resource)
            let resourceEntity = try EntityData.fromJson(jsonString: resourceStr)
            let tokenInputs: [TokenInput] = tokens.map { $0.toTokenInput() }
            let contextStr = Helper.dictionaryToString(context)
            let contextJson = JsonValue(contextStr)
            
            let result: MultiIssuerAuthorizeResult = try instance.authorizeMultiIssuer(tokens:  tokenInputs,
                                                           action: action,
                                                           resource: resourceEntity,
                                                           context: contextJson)

            
            let logs = try instance.getLogsByRequestIdAndTag(
                requestId: result.requestId,
                tag: selectedLogTypeOption
            )
            
            // Update UI with decision and principals (no person/workload)
            resultMessage = "Authorization Result: \(result.decision)"
            modelTextField = resultMessage
            let diagnosticsReason = Array(result.response.diagnostics.reasons).sorted().joined(separator: ", ")
            let diagnosticsErrors = Array(result.response.diagnostics.errors).sorted().joined(separator: ", ")
            modelJsonField = """
            {
                "decision": \(result.decision),
                "requestId": "\(result.requestId)",
                "diagnosticsReason": "\(diagnosticsReason)",
                "diagnosticsErrors": "\(diagnosticsErrors)"
            }
            """
            modelJsonLogField = Helper.processLogs(logs: logs)
            
            // Finalize state
            isAuthzRequest = true
            self.authzRequestType = authzRequestType
            showModal = true
            
        } catch {
            let message = "Unexpected error occurred: \(error)"
            print("❌ \(message)")
            resultMessage = message
            modelTitle = "Authorization Error"
            modelTextField = message
            modelJsonField = ""
            modelJsonLogField = ""
            isAuthzRequest = false
            self.authzRequestType = .none
            showModal = true
        }
    }
    
    
    struct DataButton: View {
        var title: String
        var data: Any
        
        @State private var showModal = false
        @State private var modelTextField = ""
        
        var body: some View {
            Button {
                modelTextField = Helper.dictionaryToString([title: data])
                showModal = true
            } label: {
                RoundedRectangle(cornerRadius: 25)
                    .fill(Color.white)
                    .frame(height: 50)
                    .overlay(Text(title).foregroundColor(.black))
            }
            .sheet(isPresented: $showModal) {
                ModalView(
                    title: .constant(title),
                    modelTextField: $modelTextField,
                    modelJsonField: .constant(""),
                    modelJsonLogField: .constant(""),
                    isAuthzRequest: .constant(false),
                    authzRequestType: .constant(.none)
                )
            }
        }
    }
    
    struct ActionButton: View {
        var title: String
        var color: Color
        var action: () -> Void
        
        var body: some View {
            Button(action: action) {
                Text(title)
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(color)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            .padding(.horizontal)
        }
    }
    
    struct ModalView: View {
        @Environment(\.presentationMode) var presentationMode
        @Binding var title: String
        @Binding var modelTextField: String
        @Binding var modelJsonField: String
        @Binding var modelJsonLogField: String
        @Binding var isAuthzRequest: Bool
        @Binding var authzRequestType: ContentView.AuthzRequestType
        
        var body: some View {
            VStack {
                if isAuthzRequest {
                    switch authzRequestType {
                    case .result:
                        Text(title).font(.title).padding()
                        Text("\(modelTextField)").padding()
                        DataParserView(title: Labels.MORE_INFO, jsonString: modelJsonField)
                    case .logs:
                        DataParserView(title: nil, jsonString: modelJsonLogField)
                    default:
                        EmptyView()
                    }
                } else {
                    Text(title).font(.title).padding()
                    CustomTextField(title: title, text: $modelTextField, isMultiline: true)
                }
                
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
    
    struct ContentView_Previews: PreviewProvider {
        static var previews: some View {
            ContentView()
        }
    }
}
