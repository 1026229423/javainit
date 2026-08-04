package com.orientsec.idap.core.policycompare;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Paginated history response for policy comparison tasks. */
@Data
public class PolicyCompareTaskPageView {
    private List<PolicyCompareTaskView> items = new ArrayList<>();
    private int page;
    private int pageSize;
    private long total;
}
