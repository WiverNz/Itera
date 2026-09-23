package com.wivernz.itera.lint;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.client.api.Vendor;
import com.android.tools.lint.detector.api.Issue;
import java.util.List;

public final class BootstrapIssueRegistry extends IssueRegistry {
    @Override public Vendor getVendor() {
        return new Vendor("Itera", "com.wivernz.itera.lint", "https://github.com/WiverNz/Itera/issues", null);
    }
    @Override public int getApi() { return com.android.tools.lint.detector.api.ApiKt.CURRENT_API; }
    @Override public List<Issue> getIssues() {
        return List.of(BootstrapDetector.TEXT, BootstrapDetector.TOUCH);
    }
}
