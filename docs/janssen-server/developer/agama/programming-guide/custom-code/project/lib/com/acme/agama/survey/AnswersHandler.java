package com.acme.agama.survey;

import java.util.*;

public class AnswersHandler {
    
    private int length;
    private List<Answer> answers;

    public AnswersHandler() {
        answers = new ArrayList<>();
    }
        
    public AnswersHandler(int len) {
        this();
        length = len;
    }
    
    public Map.Entry<Boolean, Boolean> isFirstLast(int index) {
        // Static method Map.entry(K, V) is shorter but the object returned is not serializable 
        return new AbstractMap.SimpleImmutableEntry(index == 0, index == length - 1);
    }
    
    public void storeAnswer(String id, String choiceId, String extraText) {
        storeAnswer(id, Collections.singletonList(choiceId), extraText);
    }

    public void storeAnswer(String id, List<String> choiceIds, String extraText) {
        
        List<String> chIds = choiceIds == null ? Collections.emptyList() : choiceIds;
        Answer ans = getAnswer(id);
        
        if (ans == null) {
            // answer not stored yet, add it to the list 
            answers.add(new Answer(id, chIds, extraText));
        } else {
            // update this answer
            ans.setChoice(chIds);
            ans.setText(extraText);
        }

    }
    
    public void close() {        
        // do something interesting with the answers, e.g. send them to a DB,
        // service, e-mail address, etc.
    }

    public Answer getAnswer(String id) {
        return answers.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
    }
    
    public String toString() {
        return answers.toString();
    }
    
}
