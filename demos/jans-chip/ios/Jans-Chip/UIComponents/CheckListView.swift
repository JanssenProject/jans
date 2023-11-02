//
//  CheckListView.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 01.11.2023.
//

import SwiftUI

struct CheckListItem: Identifiable {
    var id = UUID()
    var isChecked: Bool = false
    var title: String
 }

struct CheckView: View {
    
    @State var isChecked: Bool = false
    
    var title:String
    
    var onSelection: ((Bool) -> Void)?
    
    func toggle() {
        isChecked = !isChecked
        onSelection?(isChecked)
    }
    
    var body: some View {
        HStack{
            Button(action: toggle) {
                Image(systemName: isChecked ? "checkmark.square" : "square")
            }
            Text(title).font(.body)
        }
    }
}

struct CheckListView: View {
    
    private var checkListData: [CheckListItem] = []
    private var onSelection: ((CheckListItem, Bool) -> Void)?
    
    init(checkListData: [CheckListItem], onSelection: ((CheckListItem, Bool) -> Void)?) {
        self.checkListData = checkListData
        self.onSelection = onSelection
    }
    
    var body: some View {
        List(checkListData) { item in
            CheckView(isChecked: item.isChecked, title: item.title, onSelection: { isChecked in
                onSelection?(item, isChecked)
            })
        }
        .listStyle(.grouped)
        .font(.title)
    }
}

struct CheckListView_Previews: PreviewProvider {
    static var previews: some View {
        CheckListView(checkListData: [
            CheckListItem(title: "Task 1"),
            CheckListItem(title: "Task 2"),
            CheckListItem(title: "Task 3")
        ], onSelection: nil)
    }
}
