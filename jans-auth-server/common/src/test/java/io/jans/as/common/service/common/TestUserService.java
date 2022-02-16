package io.jans.as.common.service.common;

import java.util.ArrayList;
import java.util.List;

public class TestUserService extends UserService {

    private boolean returnTestListPersonCustomObjectClassList = false;
    public static final String[] DEFAULT_PERSON_CUSTOM_OBJECT_CLASS_LIST = {"personCustomObjectClass1", "personCustomObjectClass2"};

    public void setReturnTestListPersonCustomObjectClassList(boolean returnTestListPersonCustomObjectClassList) {
        this.returnTestListPersonCustomObjectClassList = returnTestListPersonCustomObjectClassList;
    }

    @Override
    public List<String> getPersonCustomObjectClassList() {
        List<String> result = new ArrayList<>();
        if (returnTestListPersonCustomObjectClassList) {
            result.add(DEFAULT_PERSON_CUSTOM_OBJECT_CLASS_LIST[0]);
            result.add(DEFAULT_PERSON_CUSTOM_OBJECT_CLASS_LIST[1]);
        }
        return result;
    }

    @Override
    public String getPeopleBaseDn() {
        return "baseDnTest";
    }

}
