<script type="text/ng-template" id="spam-error-message-ng2-template">
    <div style="padding: 20px;">
        <p><@orcid.msg 'public_profile.spamErrorMessage'/></p>
        <br />
        <div class="row">
            <div class="col-xs-6">
                <button class="spam-button-left btn btn-white-no-border" ><@orcid.msg 'freemarker.btnContactSupport'/></button>
            </div>
            <div class="col-xs-6">
                <button class="spam-button btn btn-white-no-border" (click)="close()"><@orcid.msg 'freemarker.btnclose'/></button>
            </div>
        </div>
    </div>
</script>