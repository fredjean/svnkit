/*
 * ====================================================================
 * Copyright (c) 2004-2008 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.test.tests.merge.ext;

import java.io.File;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNCopyTask;
import org.tmatesoft.svn.core.wc.SVNEditorAction;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.test.util.SVNTestDebugLog;
import org.tmatesoft.svn.test.wc.SVNTestFileDescriptor;
import org.tmatesoft.svn.test.wc.SVNWCDescriptor;

/**
 * @author TMate Software Ltd.
 * @version 1.2.0
 */
public class ChangeMergeTargetCopyFromPathTest extends AbstractExtMergeTest {

    public ISVNTestExtendedMergeCallback getFeatureModeCallback() {
        return new FeatureModeCallback();
    }

    public ISVNTestExtendedMergeCallback getReleaseModeCallback() {
        return new ReleaseModeCallback();
    }

    public String getDumpFile() {
        return null;
    }

    public SVNWCDescriptor getInitialFS() {
        SVNWCDescriptor fs = new SVNWCDescriptor();
        fs.addFile("A");
        fs.addFile("A/file", "this is A/file");
        return fs;
    }

    public void run() throws SVNException {
        fill();

        createWCs();
        initializeMergeCallback();

        getEnvironment().copy(getBranchFile("A/file"), SVNRevision.WORKING, getBranchFile("A/file2"), true, false, true);
        getEnvironment().commit(getBranchFile("A"), "A/file renamed to A/file2", SVNDepth.INFINITY);

        getEnvironment().modifyAndCommit(getBranchFile("A/file2"));

        getEnvironment().setEventHandler(SVNTestDebugLog.getEventHandler());

        mergeLastRevisions(getBranch(), getTrunkWC(), 2, SVNDepth.INFINITY, false, false);
        validateWC(getTrunkWC());
    }

// ###############  FEATURE MODE  ###################

    private class FeatureModeCallback implements ISVNTestExtendedMergeCallback {

        public void prepareMerge(SVNURL source, File target) throws SVNException {
        }

        public SVNCopyTask getTargetCopySource(SVNURL sourceUrl, long sourceRevision, long sourceMergeFromRevision, long sourceMergeToRevision, SVNURL targetUrl, long targetRevision) throws SVNException {
            if (targetUrl.getPath().endsWith("trunk/A/file2")) {
                SVNCopySource copySource = new SVNCopySource(SVNRevision.create(targetRevision), SVNRevision.create(targetRevision), getTrunk().appendPath("A/file", false));
                return SVNCopyTask.create(copySource, true);
            }
            return null;
        }

        public SVNURL[] getTrueMergeTargets(SVNURL sourceUrl, long sourceRevision, long sourceMergeFromRevision, long sourceMergeToRevision, SVNURL targetUrl, long targetRevision, SVNEditorAction action) throws SVNException {
            if (action == SVNEditorAction.DELETE && sourceUrl.getPath().endsWith("branch/A/file")) {
                return new SVNURL[0];
            }
            if (sourceUrl.getPath().endsWith("branch/A/file2")) {
                return new SVNURL[]{getTrunk().appendPath("A/file2", false)};
            }
            return null;
        }

        public SVNURL transformLocation(SVNURL sourceUrl, long sourceRevision, long targetRevision) throws SVNException {
            if (sourceUrl.getPath().endsWith("branch/A/file2")) {
                return getBranch().appendPath("A/file", false);
            }
            return null;
        }

        public SVNWCDescriptor getExpectedState() throws SVNException {
            SVNWCDescriptor descriptor = new SVNWCDescriptor();
            descriptor.addFile("A");

            String content = "this is A/file\n" +
                    "this line added to file file2 at r4.";
            SVNTestFileDescriptor file = descriptor.addFile("A/file2", content);
            file.setReplaced(false);
            file.setDeleted(false);
            file.setAdded(false);
            file.setCopyFromLocation(getTrunk().appendPath("A/file", false));
            file.setProperties(new SVNProperties());

            return descriptor;
        }
    }

// ###############  RELEASE MODE  ###################

    //Release mode for this test is not necessary. Added implementation with no practice aim

    private class ReleaseModeCallback implements ISVNTestExtendedMergeCallback {

        public void prepareMerge(SVNURL source, File target) throws SVNException {
        }

        public SVNCopyTask getTargetCopySource(SVNURL sourceUrl, long sourceRevision, long sourceMergeFromRevision, long sourceMergeToRevision, SVNURL targetUrl, long targetRevision) throws SVNException {
            if (targetUrl.getPath().endsWith("trunk/A/file")) {
                SVNCopySource copySource = new SVNCopySource(SVNRevision.UNDEFINED, SVNRevision.create(getStartRevision()), getBranch().appendPath("A/file2", false));
                return SVNCopyTask.create(copySource, true);
            }
            return null;
        }

        public SVNURL[] getTrueMergeTargets(SVNURL sourceUrl, long sourceRevision, long sourceMergeFromRevision, long sourceMergeToRevision, SVNURL targetUrl, long targetRevision, SVNEditorAction action) throws SVNException {
            if (sourceUrl.getPath().endsWith("branch/A/file2")) {
                return new SVNURL[]{getTrunk().appendPath("A/file", false)};
            }
            return null;
        }

        public SVNURL transformLocation(SVNURL sourceUrl, long sourceRevision, long targetRevision) throws SVNException {
            if (sourceUrl.getPath().endsWith("branch/A/file2")) {
                return getBranch().appendPath("A/file", false);
            }
            return null;
        }

        public SVNWCDescriptor getExpectedState() throws SVNException {
            SVNWCDescriptor descriptor = new SVNWCDescriptor();
            descriptor.addFile("A");

            String content = "this is A/file\n" +
                    "this line added to file file2 at r4.";
            SVNTestFileDescriptor file = descriptor.addFile("A/file", content);
            file.setReplaced(true);
            file.setDeleted(false);
            file.setAdded(false);
            file.setConflicted(false);
            file.setCopyFromLocation(getBranch().appendPath("A/file", false));
            file.setCopyFromRevision(getStartRevision());
            file.setProperties(new SVNProperties());

            return descriptor;
        }
    }
}