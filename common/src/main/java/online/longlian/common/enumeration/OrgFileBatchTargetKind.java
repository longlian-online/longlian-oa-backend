package online.longlian.common.enumeration;

/** 组织文件页批量下载的选中目标。 */
public enum OrgFileBatchTargetKind {
    /** 一条当前有效提交文件 */
    FILE,
    /** 一个项目下的全部当前有效提交文件 */
    ITEM,
    /** 一个企划下的全部当前有效提交文件 */
    PROJECT,
    /** 一个企划类型下的全部当前有效提交文件 */
    TYPE
}
