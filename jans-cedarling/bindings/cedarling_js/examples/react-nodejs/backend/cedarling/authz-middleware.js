function truncateToken(token) {
  if (!token || typeof token !== 'string') return '(none)';
  if (token.length <= 20) return token;
  return token.substring(0, 12) + '...' + token.substring(token.length - 8);
}

export function authorizeMiddleware(cedarling, tasks) {
  return function authorize(action, getResourceId = () => null) {
    return async (req, res, next) => {
      const userId = req.headers['x-user-id'] || 'bob';
      const resourceId = getResourceId(req);
      const task = tasks.find((t) => t.id === resourceId);
      const taskOwner = task ? task.owner : userId;

      const authHeader = req.headers['authorization'];
      const isSigned = authHeader && authHeader.startsWith('Bearer ');
      const token = isSigned ? authHeader.split(' ')[1] : null;
      const resource = {
        type: 'TaskApp::Task',
        id: resourceId || 'list-tasks',
        attributes: {
          owner: taskOwner,
          title: task ? task.title : (req.body?.title || 'untitled'),
          completed: task ? task.completed : false,
        },
      };

      console.log(`[authz] action="${action}" user="${userId}" resource="${resourceId || 'list'}" signed=${isSigned} token=${truncateToken(token)}`);

      let authResult;

      if (isSigned) {
        authResult = await cedarling.authorizeMultiIssuer({
          tokens: [
            {
              mapping: 'LocalMockIdP::Userinfo_token',
              payload: token,
            },
          ],
          action: `TaskApp::Action::"${action}"`,
          resource,
          context: {},
        });
      } else {
        authResult = await cedarling.authorizeUnsigned({
          principal: {
            type: 'TaskApp::User',
            id: userId,
          },
          action: `TaskApp::Action::"${action}"`,
          resource,
          context: { userId },
        });
      }

      if (!authResult.ok) {
        const err = authResult.error;
        const detailStr = err.details ? JSON.stringify(err.details) : '(none)';
        console.error(
          `[authz] ERROR user="${userId}" action="${action}" resource="${resourceId}" signed=${isSigned}: code=${err.code} msg=${err.message} details=${detailStr}`,
        );
        return res.status(isSigned ? 401 : 500).json({
          error: isSigned ? 'Invalid or expired token' : 'Authorization system failure',
          code: err.code,
        });
      }

      const { decision, requestId, diagnostics } = authResult.value;
      console.log(
        `[authz] DECISION=${decision ? 'ALLOW' : 'DENY'} user="${userId}" action="${action}" resource="${resourceId || 'list'}" requestId=${requestId}`,
      );
      if (!decision && diagnostics) {
        console.log(`[authz] REASONS: ${JSON.stringify(diagnostics.reasons)}`);
        if (diagnostics.errors && diagnostics.errors.length > 0) {
          for (const e of diagnostics.errors) {
            console.log(`[authz] ERROR policy="${e.policyId}" msg="${e.message}"`);
          }
        }
      }

      if (decision) {
        req.userId = userId;
        next();
      } else {
        res.status(403).json({
          error: 'Forbidden by policy',
          diagnostics: {
            reasons: diagnostics.reasons,
            errors: diagnostics.errors,
          },
        });
      }
    };
  };
}
